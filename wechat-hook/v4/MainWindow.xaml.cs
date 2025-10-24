using System.IO;
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
        public MainWindow()
        {
            InitializeComponent();
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
            comments.Clear();
            Config.Clear();
            string path = Environment.CurrentDirectory + "\\config.properties";
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
                    int i = line.IndexOf('=');;
                    var key = line.Substring(0, i);
                    var value = line.Substring(i + 1, line.Length - i - 1);
                    Config[key] = value;
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
                        if (item.Name.StartsWith("wxid_")) users.Add(item.Name);
                    }
                    if (users.Count > 1)
                    {
                        realDbFolder = "";
                        warn("当前环境有多位用户登录过微信，无法确定应该使用哪一个用户，请到左上角“文件”设置数据库路径。");
                    }
                    else
                    {
                        string dbPath = directiroy.FullName + "\\" + users[0] + "\\db_storage\\message";
                        if (!Directory.Exists(dbPath))
                        {
                            realDbFolder = "";
                            warn($"已拼接微信数据库路径 {dbPath}，但路径指向的文件夹不存在，请到左上角“文件”设置数据库路径。");
                        }
                        else
                        {
                            realDbFolder = dbPath;
                            info($"微信数据库路径 (自动选择): {realDbFolder}");
                        }
                    }
                }
            }
            else
            {
                realDbFolder = databaseFolder;
                if (realDbFolder != string.Empty)
                {
                    info($"微信数据库路径 (手动设置): {realDbFolder}");
                }
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
            var temp = Environment.CurrentDirectory + "\\.tmp";
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

            }
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
        }
    }
}
