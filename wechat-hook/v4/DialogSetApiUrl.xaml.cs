using System.Windows;

namespace WeChatHook
{
    /// <summary>
    /// DialogSetApiUrl.xaml 的交互逻辑
    /// </summary>
    public partial class DialogSetApiUrl : Window
    {
        public string Text { get; private set; }
        public DialogSetApiUrl(string text)
        {
            InitializeComponent();
            TextInput.Text = text;
            Text = text;
        }

        private void Button_OK_Click(object sender, RoutedEventArgs e)
        {
            Text = TextInput.Text;
            DialogResult = true;
            Close();
        }
    }
}
