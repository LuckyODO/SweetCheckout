﻿using Microsoft.Win32;
using System.IO;
using System.Windows;
using System.Windows.Controls;

namespace WeChatHook
{
    /// <summary>
    /// DialogSetApiUrl.xaml 的交互逻辑
    /// </summary>
    public partial class DialogSetDatabaseFolder : Window
    {
        public string Text { get; private set; }
        public DialogSetDatabaseFolder(string text)
        {
            InitializeComponent();
            TextInput.Text = text;
            Text = text;
            AddToAutoDetectList(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments) + "\\xwechat_files");
            foreach (var drive in Environment.GetLogicalDrives())
            {
                if (Directory.Exists(drive + "xwechat_files"))
                {
                    AddToAutoDetectList(drive + "xwechat_files");
                }
            }
        }

        private void AddToAutoDetectList(string xwechat_files)
        {
            var directory = new DirectoryInfo(xwechat_files);
            foreach (var item in directory.GetDirectories())
            {
                string dbPath = directory.FullName + "\\" + item.Name + "\\db_storage\\message";
                if (Directory.Exists(dbPath))
                {
                    AutoDetectList.Items.Add(new ListBoxItem
                    {
                        Content = item.Name,
                        Tag = dbPath
                    });
                }
            }
        }

        private void Button_OK_Click(object sender, RoutedEventArgs e)
        {
            Text = TextInput.Text;
            DialogResult = true;
            Close();
        }

        private void Button_Browse_Click(object sender, RoutedEventArgs e)
        {
            var ofd = new OpenFolderDialog
            {
                Title = "选择 “xwechat_files\\(用户文件夹)\\db_storage\\message”",
                InitialDirectory = Text == string.Empty ? (Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments) + "\\xwechat_files") : Text,
                Multiselect = false
            };
            if (ofd.ShowDialog() == true)
            {
                TextInput.Text = ofd.FolderName;
            }
        }

        private void AutoDetectList_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (AutoDetectList.SelectedItem is ListBoxItem item)
            {
                var tag = item.Tag;
                if (tag is string dbPath && dbPath != string.Empty)
                {
                    TextInput.Text = dbPath;
                }
            }
        }
    }
}
