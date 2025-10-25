﻿using Microsoft.Data.Sqlite;
using Microsoft.Win32;
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
            Mode = SqliteOpenMode.ReadWriteCreate,
            Cache = SqliteCacheMode.Shared,
            Pooling = false,
        }.ToString();
        private HashSet<string> ProcessingFiles = [], NextProcessFiles = [];
        public MainWindow()
        {
            InitializeComponent();
            watcher.NotifyFilter = NotifyFilters.LastWrite | NotifyFilters.CreationTime;
            watcher.IncludeSubdirectories = false;
            watcher.Created += OnFileChanged;
            watcher.Changed += OnFileChanged;
        }

        private async void OnFileChanged(object sender, FileSystemEventArgs e)
        {
            var fi = new FileInfo(e.FullPath);
            var fileName = fi.Name;
            if (fi.Exists && fi.Name.StartsWith("biz_message_"))
            {
                if (hexKey == string.Empty) return;
                if (fileName.EndsWith(".db"))
                {
                    info($"检测到聊天数据变更 {fi.Name}");
                    await OnFileChange(fi);
                }
            }
        }

        private async Task OnFileChange(FileInfo fi)
        {
            var fullPath = fi.FullName;
            var target = temp + "\\" + fi.Name.Replace(".db", "") + "-" + Guid.NewGuid() + ".db";
            if (ProcessingFiles.Contains(fullPath))
            {
                NextProcessFiles.Add(fullPath);
                return;
            }
            List<Message> scan;
            try
            {
                DecryptService.DecryptDatabase(hexKey, fi.FullName, target);
                scan = await DatabaseService.Scan(target);
                ProcessingFiles.Remove(fullPath);
            }
            catch (Exception ex)
            {
                warn($"(文件监听) {ex}");
                ProcessingFiles.Remove(fullPath);
                if (NextProcessFiles.Remove(fullPath)) OnFileChange(fi);
                DeleteDatabaseFile(target);
                return;
            }
            if (NextProcessFiles.Remove(fullPath))
            {
                DeleteDatabaseFile(target);
                OnFileChange(fi);
            }
            else
            {
                handleMessageSubmit(scan);
                DeleteDatabaseFile(target);
            }
        }

        private void handleMessageSubmit(ICollection<Message> messages, bool submitToBackend = true)
        {
            if (messages.Count == 0) return;
            lock (storageConnectionString)
            {
                // 筛选并提交未处理的收款消息到后端
                var outdate = DateTimeOffset.UtcNow.AddMinutes(-DatabaseService.RecentMinutes);
                var timestamp = outdate.ToUnixTimeSeconds();
                try
                {
                    using var conn = new SqliteConnection(storageConnectionString);
                    conn.Open();
                    var list = new List<Message>();
                    foreach (var item in messages)
                    {
                        // 太早的消息不进行处理
                        if (item.CreateTime <= outdate) continue;
                        list.Add(item);
                    }
                    using var cmd = new SqliteCommand($"SELECT * FROM 'handled_sequences' WHERE create_time > {timestamp};", conn);
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
                        if (!submitToBackend) continue;
                        info($"提交收款记录到后端: ￥{message.Money}");
                        // 将消息发送给后端
                        if (apiUrl != string.Empty)
                        {
                            try
                            {
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
                            catch (Exception ex)
                            {
                                error($"提交收款记录时发生错误: {ex.Message}");
                            }
                        }
                        else
                        {
                            warn("未设置后端 API 地址，无法提交收款记录");
                        }
                    }
                    conn.Close();
                }
                catch (Exception ex)
                {
                    error($"处理收款记录时发生错误: {ex.Message}");
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
            CheckWatcherStatus();
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

        private void CheckWatcherStatus()
        {
            var oldStatus = watcher.EnableRaisingEvents;
            if (realDbFolder != string.Empty && hexKey != string.Empty)
            {
                watcher.Path = realDbFolder;
                watcher.EnableRaisingEvents = true;
                if (!oldStatus)
                {
                    info("聊天数据文件监视器已开启");
                    Task.Run(async () => await ScanAll(false));
                }
            }
            else
            {
                watcher.EnableRaisingEvents = false;
                if (oldStatus) info("聊天数据文件监视器已关闭");
            }
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            ReloadConfig();
            SaveConfig();
        }

        private async Task ScanAll(bool submit)
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
            info("开始扫描所有聊天数据文件…");
            var messages = new HashSet<Message>();
            var directory = new DirectoryInfo(realDbFolder);
            var warning = true;
            foreach (var file in directory.GetFiles())
            {
                if (file.Name.StartsWith("biz_message_") && file.Name.EndsWith(".db"))
                {
                    warning = false;
                    try
                    {
                        var target = temp + "\\" + file.Name.Replace(".db", "") + "-" + Guid.NewGuid() + ".db";
                        DecryptService.DecryptDatabase(hexKey, file.FullName, target);
                        var scan = await DatabaseService.Scan(target);
                        foreach (var message in scan)
                        {
                            messages.Add(message);
                        }
                        DeleteDatabaseFile(target);
                    }
                    catch (Exception ex)
                    {
                        error($"(扫描全部) 处理 {file.Name} 时发生错误: ${ex}");
                    }
                }
            }
            if (warning)
            {
                warn($"在数据库文件夹下没有发现 .db 文件，你确定你输入的路径是正确的吗？");
            }
            else
            {
                info($"共发现最近 {DatabaseService.RecentMinutes} 分钟内有 {messages.Count} 条收款记录");
            }

            handleMessageSubmit(messages, submit);
        }

        private void DeleteDatabaseFile(string target)
        {
            if (File.Exists(target)) File.Delete(target);
            if (File.Exists(target + "-shm")) File.Delete(target + "-shm");
            if (File.Exists(target + "-wal")) File.Delete(target + "-wal");
        }

        private void PutHandled(SqliteConnection conn, Message message)
        {
            debug($"提交 {message.SenderId} 在 {message.CreateTime.ToString("yyyy-MM-dd HH:mm:ss")} 的消息 {message.ServerSequence} 为已处理: {message.MessageContent.Replace("\n", "").Replace("\r", "")}");
            using var cmd = new SqliteCommand("INSERT INTO 'handled_sequences'(`server_seq`,`sender_id`,`create_time`) VALUES($server_seq,$sender_id,$create_time);", conn);
            cmd.Parameters.Add("$server_seq", SqliteType.Integer).Value = Convert.ToInt64(message.ServerSequence);
            cmd.Parameters.Add("$sender_id", SqliteType.Text).Value = message.SenderId;
            cmd.Parameters.Add("$create_time", SqliteType.Integer).Value = message.CreateTime.ToUnixTimeSeconds();
            cmd.ExecuteNonQuery();
        }

        private SolidColorBrush brushLogNormal = new SolidColorBrush(Colors.Black);
        private SolidColorBrush brushLogWarning = new SolidColorBrush(Colors.Orange);
        private SolidColorBrush brushLogError = new SolidColorBrush(Colors.Red);
        enum LogLevel
        {
            Debug,
            Info,
            Warning,
            Error
        }
        private string GetLogLevelString(LogLevel level)
        {
            return level switch
            {
                LogLevel.Debug => "[调试]",
                LogLevel.Info => "[信息]",
                LogLevel.Warning => "[警告]",
                LogLevel.Error => "[错误]",
                _ => ""
            };
        }
        private void log(LogLevel level, string message)
        {
            DateTime time = DateTime.Now;
            var inlines = TextLogs.Inlines;
            var levelStr = GetLogLevelString(level);
            var show = level >= LogLevel.Info;

            if (show) Dispatcher.Invoke(() =>
            {
                inlines.Add(new Run(time.ToString("[MM-dd HH:mm:ss] ")) { Foreground = brushLogNormal });
                switch (level)
                {
                    case LogLevel.Info:
                        inlines.Add(new Run(levelStr) { Foreground = brushLogNormal, FontWeight = FontWeights.Bold });
                        break;
                    case LogLevel.Warning:
                        inlines.Add(new Run(levelStr) { Foreground = brushLogWarning, FontWeight = FontWeights.Bold });
                        break;
                    case LogLevel.Error:
                        inlines.Add(new Run(levelStr) { Foreground = brushLogError, FontWeight = FontWeights.Bold });
                        break;
                }
                TextLogs.Inlines.Add(new Run(" " + message) { Foreground = brushLogNormal });
                TextLogs.Inlines.Add(new LineBreak());
            });
            string directory = Environment.CurrentDirectory + "\\logs\\";
            string filePath = directory + time.ToString("yyyy-MM-dd") + ".log";
            if (!Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }
            string content = $"{time.ToString("[HH:mm:ss]")} {levelStr} {message}";
            File.AppendAllText(filePath, content + Environment.NewLine, utf8);
        }

        private void debug(string message) => log(LogLevel.Debug, message);
        private void info(string message) => log(LogLevel.Info, message);
        private void warn(string message) => log(LogLevel.Warning, message);
        private void error(string message) => log(LogLevel.Error, message);

        private void SetApiUrl_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new DialogSetApiUrl(apiUrl);
            if (dialog.ShowDialog() == true)
            {
                apiUrl = dialog.Text;
                SaveConfig();
                CheckWatcherStatus();
            }
        }

        private void SetWeChatKey_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new DialogSetKey(hexKey);
            if (dialog.ShowDialog() == true)
            {
                hexKey = dialog.Text;
                SaveConfig();
                CheckWatcherStatus();
            }
        }
        private void SetWeChatDatabaseFolder_Click(object sender, RoutedEventArgs e)
        {
            var dialog = new DialogSetDatabaseFolder(realDbFolder);
            if (dialog.ShowDialog() == true)
            {
                databaseFolder = dialog.Text;
                SaveConfig();
                ReloadConfig();
            }
        }
        private void ReloadConfig_Click(object sender, RoutedEventArgs e)
        {
            ReloadConfig();
            SaveConfig();
        }
        private void ToolsCustomDecrypt_Click(object sender, RoutedEventArgs e)
        {
            var ofd = new OpenFileDialog
            {
                Title = "选择要解密的微信数据库文件",
                Filter = "微信数据库文件 (*.db)|*.db",
                Multiselect = false,
                InitialDirectory = realDbFolder == string.Empty ? (Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments) + "\\xwechat_files") : realDbFolder,
            };
            if (ofd.ShowDialog() == true)
            {
                var sfd = new SaveFileDialog
                {
                    Title = "选择解密后的保存位置",
                    Filter = "SQLite 文件 (*.db)|*.db",
                    FileName = Path.GetFileNameWithoutExtension(ofd.FileName) + "-decrypted.db",
                    InitialDirectory = Environment.CurrentDirectory,
                };
                if (sfd.ShowDialog() == true)
                {
                    try
                    {
                        DecryptService.DecryptDatabase(hexKey, ofd.FileName, sfd.FileName);
                        MessageBox.Show($"已将微信数据库文件解密并保存到\n{sfd.FileName}");
                    }
                    catch (Exception ex)
                    {
                        MessageBox.Show($"解密微信数据库文件时发生错误:\n {ex}");
                    }
                }
            }
        }

        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            watcher.EnableRaisingEvents = false;
            watcher.Dispose();
            var directory = new DirectoryInfo(temp);
            foreach (var file in directory.GetFiles())
            {
                try
                {
                    file.Delete();
                }
                catch { }
            }
        }

        private void ButtonClear_Click(object sender, RoutedEventArgs e)
        {
            TextLogs.Inlines.Clear();
        }
    }
}
