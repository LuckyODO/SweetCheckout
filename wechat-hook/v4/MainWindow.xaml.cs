using Microsoft.Data.Sqlite;
using System.Data;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text;
using System.Windows;
using System.Windows.Documents;
using System.Windows.Media;

namespace WeChatHook
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        UTF8Encoding utf8 = new UTF8Encoding(false);
        Dictionary<string, string> Config = [];
        List<string> comments = [];
        private string apiUrl = "";
        private string hexKey = "";
        private string databaseFolder = "";
        private string realDbFolder = "";
        string temp = Environment.CurrentDirectory + "\\.tmp";
        FileSystemWatcher watcher = new FileSystemWatcher();
        string storageConnectionString = new SqliteConnectionStringBuilder
        {
            DataSource = Environment.CurrentDirectory + "\\storage.db",
            Mode = SqliteOpenMode.ReadWriteCreate
        }.ToString();
        public MainWindow()
        {
            InitializeComponent();
            watcher.NotifyFilter = NotifyFilters.LastWrite | NotifyFilters.CreationTime;
            watcher.IncludeSubdirectories = false;
            watcher.Created += OnFileChanged;
            watcher.Changed += OnFileChanged;
        }

        private void OnFileChanged(object sender, FileSystemEventArgs e)
        {
            var fi = new FileInfo(e.FullPath);
            var fileName = fi.Name;
            if (fi.Exists && fi.Name.StartsWith("biz_message_"))
            {
                if (hexKey == string.Empty) return;
                if (fileName.EndsWith(".db"))
                {
                    var target = temp + "\\" + fi.Name;
                    DecryptService.DecryptDatabase(hexKey, fi.FullName, target);
                    var scan = DatabaseService.Scan(target);
                    handleMessageSubmit(scan);
                }
            }
        }

        private void handleMessageSubmit(ICollection<Message> messages)
        {
            lock (storageConnectionString)
            {
                // 筛选并提交未处理的收款消息到后端
                var outdate = DateTimeOffset.UtcNow.AddMinutes(-15);
                var timestamp = outdate.ToUnixTimeSeconds();
                using (var conn = new SqliteConnection(storageConnectionString))
                {
                    conn.Open();
                    var list = new List<Message>();
                    foreach (var item in messages)
                    {
                        // 太早的消息不进行处理
                        if (item.CreateTime <= outdate) continue;
                        list.Add(item);
                    }
                    using var cmd = new SqliteCommand($"SELECT * FROM 'handled_sequences' WHERE create_time > {timestamp};");
                    var reader = cmd.ExecuteReader();
                    var handled = new List<string>();
                    foreach (var item in reader)
                    {
                        if (item is not IDataRecord record) continue;
                        var server_seq = record["server_seq"].ToString();
                        var sender_id = record["sender_id"].ToString();
                        if (server_seq == null || sender_id == null) continue;
                        handled.Add(sender_id + ";" + server_seq);
                    }
                    foreach (var message in list)
                    {
                        // 已经在数据库里的消息不进行处理
                        var key = message.SenderId + ";" + message.ServerSequence;
                        if (handled.Contains(key)) continue;
                        // 提交为已处理消息
                        PutHandled(conn, message);
                        // 将消息发送给后端
                        var client = new HttpClient();
                        var request = new HttpRequestMessage(HttpMethod.Post, new Uri(apiUrl));
                        request.Content = JsonContent.Create(new
                        {
                            type = "wechat",
                            flag = message.SenderId == "gh_3dfda90e39d6" ? "reawrd-code" : "",
                            money = message.Money ?? "",
                        });
                        client.Send(request);
                    }
                }
            }
        }

        private string? GetFromConfig(string name)
        {
            if (Config.TryGetValue(name, out string? value))
            {
                return value;
            }
            return null;
        }

        private void ReloadConfig()
        {
            using (var conn = new SqliteConnection(storageConnectionString))
            {
                conn.Open();
                using var cmd = new SqliteCommand("CREATE TABLE if NOT EXISTS 'handled_sequences'(`server_seq` INT32, `sender_id` VARCHAR(24), `create_time` INT32);", conn);
                cmd.ExecuteNonQuery();
            }
            comments.Clear();
            Config.Clear();
            string path = Environment.CurrentDirectory + "\\config.properties";
            if (File.Exists(path)) {
                bool flag = false;
                foreach (var line in File.ReadAllLines(path, utf8))
                {
                    if (line.Trim().StartsWith('#'))
                    {
                        if (!flag) comments.Add(line);
                        continue;
                    }
                    if (line.Contains('='))
                    {
                        flag = true;
                        int i = line.IndexOf('='); ;
                        var key = line.Substring(0, i);
                        var value = line.Substring(i + 1, line.Length - i - 1);
                        Config[key] = value;
                    }
                }
            }
            apiUrl = GetFromConfig("api_url") ?? "";
            hexKey = GetFromConfig("wechat_key") ?? "";
            databaseFolder = GetFromConfig("database_folder") ?? "auto";
            info("配置文件已重载");
            if (databaseFolder == "auto")
            {
                var directiroy = new DirectoryInfo(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments) + "\\xwechat_files");
                if (!directiroy.Exists)
                {
                    realDbFolder = "";
                    warn("找不到微信数据文件夹 xwechat_files，请到左上角“文件”设置数据库路径。");
                }
                else
                {
                    var users = new List<string>();
                    foreach (var item in directiroy.GetDirectories())
                    {
                        var fileName = item.Name;
                        if (fileName == "all_users" || fileName == "Backup" || fileName == "WMPF") continue;
                        string dbPath = directiroy.FullName + "\\" + item.Name + "\\db_storage\\message";
                        if (Directory.Exists(dbPath)) users.Add(dbPath);
                    }
                    realDbFolder = users.FirstOrDefault(string.Empty);
                    if (realDbFolder == string.Empty)
                    {
                        warn("当前环境有多位用户登录过微信，无法确定应该使用哪一个用户，请到左上角“文件”设置数据库路径。");
                    }
                    else
                    {
                        info($"微信数据库路径 (自动选择): {realDbFolder}");
                    }
                }
            }
            else
            {
                realDbFolder = databaseFolder.Trim();
                if (realDbFolder != string.Empty)
                {
                    info($"微信数据库路径 (手动设置): {realDbFolder}");
                }
            }

            if (realDbFolder != string.Empty)
            {
                watcher.Path = realDbFolder;
                watcher.EnableRaisingEvents = true;
            } else
            {
                watcher.EnableRaisingEvents = false;
            }
        }

        private void SaveConfig()
        {
            string path = Environment.CurrentDirectory + "\\config.properties";

            Config["api_url"] = apiUrl;
            Config["wechat_key"] = hexKey;
            Config["database_folder"] = databaseFolder;

            var lines = new List<string>();
            lines.AddRange(comments);
            foreach (var pair in Config)
            {
                lines.Add(pair.Key + "=" + pair.Value);
            }
            File.WriteAllLines(path, lines, utf8);
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            ReloadConfig();
            SaveConfig();
            Dispatcher.InvokeAsync(() => ScanAll(false));
        }

        private void ScanAll(bool submit)
        {
            if (hexKey == string.Empty)
            {
                error("未设置微信密钥");
                return;
            }
            if (realDbFolder == string.Empty)
            {
                error("未设置微信数据库路径");
                return;
            }
            var messages = new HashSet<Message>();
            var directory = new DirectoryInfo(realDbFolder);
            foreach (var file in directory.GetFiles())
            {
                if (file.Name.StartsWith("biz_message_") && file.Name.EndsWith(".db"))
                {
                    try
                    {
                        var target = temp + "\\" + file.Name;
                        DecryptService.DecryptDatabase(hexKey, file.FullName, target);
                        var scan = DatabaseService.Scan(target);
                        foreach (var message in scan)
                        {
                            messages.Add(message);
                        }
                    }
                    catch (Exception ex)
                    {
                        error($"处理 {file.Name} 时发生错误: ${ex.Message}");
                    }
                }
            }
            info($"共发现 {messages.Count} 条收款记录");

            if (submit)
            {
                handleMessageSubmit(messages);
            }
        }

        private void PutHandled(SqliteConnection conn, Message message)
        {
            using var cmd = new SqliteCommand("INSERT INTO 'handled_sequences'(`server_seq`,`sender_id`,`create_time`) VALUES($server_seq,$sender_id,$create_time);", conn);
            cmd.Parameters.Add("$server_seq", SqliteType.Integer).Value = Convert.ToInt64(message.ServerSequence);
            cmd.Parameters.Add("$sender_id", SqliteType.Text).Value = message.SenderId;
            cmd.Parameters.Add("$create_time", SqliteType.Integer).Value = message.CreateTime.ToUnixTimeSeconds();
            cmd.ExecuteNonQuery();
        }

        private SolidColorBrush brushLogNormal = new SolidColorBrush(Colors.Black);
        private SolidColorBrush brushLogWarning = new SolidColorBrush(Colors.Yellow);
        private SolidColorBrush brushLogError = new SolidColorBrush(Colors.Red);
        enum LogLevel
        {
            Info,
            Warning,
            Error
        }
        private void log(LogLevel level, string message)
        {
            var inlines = TextLogs.Inlines;
            inlines.Add(new Run(DateTime.Now.ToString("[MM-dd HH:mm:ss] ")) { Foreground = brushLogNormal });
            switch(level)
            {
                case LogLevel.Info:
                    inlines.Add(new Run("[信息]") { Foreground = brushLogNormal, FontWeight = FontWeights.Bold });
                    break;
                case LogLevel.Warning:
                    inlines.Add(new Run("[警告]") { Foreground = brushLogWarning, FontWeight = FontWeights.Bold });
                    break;
                case LogLevel.Error:
                    inlines.Add(new Run("[错误]") { Foreground = brushLogError, FontWeight = FontWeights.Bold });
                    break;
            }
            TextLogs.Inlines.Add(new Run(" " + message) { Foreground = brushLogNormal });
            TextLogs.Inlines.Add(new LineBreak());
        }

        private void info(string message) => log(LogLevel.Info, message);

        private void warn(string message) => log(LogLevel.Warning, message);

        private void error(string message) => log(LogLevel.Error, message);

        private void SetWeChatKey_Click(object sender, RoutedEventArgs e)
        {

        }
        private void SetWeChatDatabaseFolder_Click(object sender, RoutedEventArgs e)
        {

        }
        private void ReloadConfig_Click(object sender, RoutedEventArgs e)
        {
            ReloadConfig();
            SaveConfig();
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            watcher.EnableRaisingEvents = false;
            watcher.Dispose();
        }
    }
}
