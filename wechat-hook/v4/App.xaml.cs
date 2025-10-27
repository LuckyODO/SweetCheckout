using System.Configuration;
using System.Data;
using System.IO;
using System.Text;
using System.Windows;
using System.Windows.Threading;

namespace WeChatHook
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);
            DispatcherUnhandledException += (sender, e) => HandleException(e.Exception);
            AppDomain.CurrentDomain.UnhandledException += (sender, e) => HandleException((Exception) e.ExceptionObject);
            TaskScheduler.UnobservedTaskException += (sender, e) => HandleException(e.Exception);
            Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);
        }

        private void HandleException(Exception ex)
        {
            var encoding = new UTF8Encoding(false);
            File.WriteAllText("LastError.log", ex.ToString(), encoding);
        }

    }

}
