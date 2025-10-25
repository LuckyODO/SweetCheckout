using System.Diagnostics;
using System.Windows;

namespace WeChatHook
{
    /// <summary>
    /// DialogSetKey.xaml 的交互逻辑
    /// </summary>
    public partial class DialogSetKey : Window
    {
        public string Text { get; private set; }
        public DialogSetKey(string text)
        {
            InitializeComponent();
            TextInput.Text = text;
            Text = text;
        }

        private void Button_Help_Click(object sender, RoutedEventArgs e)
        {
            Process.Start("explorer", "https://github.com/ycccccccy/wx_key");
        }

        private void Button_OK_Click(object sender, RoutedEventArgs e)
        {
            Text = TextInput.Text;
            DialogResult = true;
            Close();
        }
    }
}
