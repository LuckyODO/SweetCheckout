#include "Shlwapi.h"
#include "framework.h"
#include "resource.h"
#include <filesystem>
#include <process.h>
#include <tlhelp32.h>
#include <windows.h>
#include <stdio.h>

#include "injector.h"
#include "main.h"
#include "util.h"
#include "spy.h"
#include "log.h"

static BOOL injected              = false;
static HANDLE wcProcess           = NULL;
static HMODULE spyBase            = NULL;
static WCHAR spyDllPath[MAX_PATH] = { 0 };
static HWND dlg                   = NULL;

static BOOL SaveSpyDll(wchar_t* savePath)
{
    HRSRC hRsrc = FindResource(NULL, MAKEINTRESOURCE(IDR_BIN1), TEXT("BIN"));
    if (NULL == hRsrc)
        return FALSE;
    DWORD dwSize = SizeofResource(NULL, hRsrc);
    if (0 == dwSize)
        return FALSE;
    HGLOBAL hGlobal = LoadResource(NULL, hRsrc);
    if (NULL == hGlobal)
        return FALSE;
    LPVOID pBuffer = LockResource(hGlobal);
    if (NULL == pBuffer)
        return FALSE;

    HANDLE hFile = CreateFile(savePath, GENERIC_WRITE, FILE_SHARE_WRITE, NULL,
        CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (hFile == INVALID_HANDLE_VALUE)
    {
        return FALSE;
    }
    DWORD dwWrite = NULL;
    BOOL result = WriteFile(hFile, pBuffer, dwSize, &dwWrite, NULL);
    CloseHandle(hFile);

    FreeResource(hGlobal);
    return result;
}

static int GetDllPath(wchar_t *dllPath)
{
    wchar_t temp_file_path[MAX_PATH];
    lstrcpy(temp_file_path, std::filesystem::current_path().wstring().c_str());
    PathAppend(temp_file_path, L".temp");
    CreateDirectory(temp_file_path, NULL);
    SetFileAttributes(temp_file_path, FILE_ATTRIBUTE_SYSTEM | FILE_ATTRIBUTE_HIDDEN);
    PathAppend(temp_file_path, L"SweetCheckout.Hook.WeChat.dll");

    if (!SaveSpyDll(temp_file_path)) {
        LOG_WARN("DLL 保存失败");
        MessageBox(NULL, L"DLL保存失败", L"错误", 0);
        return ERROR_FILE_NOT_SUPPORTED;
    }

    lstrcpy(dllPath, temp_file_path);

    if (!PathFileExists(dllPath)) {
        LOG_WARN("DLL 文件不存在: {}", Wstring2String(dllPath));
        MessageBox(NULL, dllPath, L"文件不存在", 0);
        return ERROR_FILE_NOT_FOUND;
    }

    return 0;
}

int WxInitInject(bool debug)
{
    bool firstOpen = true;
    int status  = 0;
    DWORD wcPid = 0;
    std::map<std::string, std::string> properties = read_properties("config.properties");

    if (properties.count("api_url") == 0)
    {
        LOG_WARN("[WxInitInject] 无法读取到配置");
        MessageBox(NULL, L"无法读取到配置", L"WxInitInject", 0);
        return -1;
    }

    status = GetDllPath(spyDllPath);
    if (status != 0) {
        return status;
    }
    
    LOG_INFO("[WxInitInject] 已找到 spy.dll 路径: {}", Wstring2String(spyDllPath));

    status = OpenWeChat(&wcPid, &firstOpen);
    if (status != 0) {
        LOG_WARN("[WxInitInject] 微信打开失败");
        MessageBox(NULL, L"打开微信失败", L"WxInitInject", 0);
        return status;
    }

    LOG_INFO("微信 PID: {}", to_string(wcPid));

    if (!IsProcessX64(wcPid)) {
        LOG_WARN("[WxInitInject] 只支持 64 位微信");
        MessageBox(NULL, L"只支持 64 位微信", L"WxInitInject", 0);
        return -1;
    }

    if (firstOpen) {
        LOG_INFO("[WxInitInject] 正在等待微信启动");
        if (dlg != NULL) SetDlgItemTextA(dlg, ID_DLL_NAME, "正在等待微信启动");
        Sleep(2000);
    }
    wcProcess = InjectDll(wcPid, spyDllPath, &spyBase);
    if (wcProcess == NULL) {
        LOG_WARN("[WxInitInject] 注入失败");
        MessageBox(NULL, L"注入失败", L"WxInitInject", 0);
        return -1;
    }

    PortPath_t pp = { 0 };
    strcpy_s(pp.baseUrl, MAX_PATH, properties.at("api_url").c_str());
    sprintf_s(pp.path, MAX_PATH, "%s", std::filesystem::current_path().string().c_str());

    if (!CallDllFuncEx(wcProcess, spyDllPath, spyBase, "InitSpy", (LPVOID)&pp, sizeof(PortPath_t), NULL)) {
        LOG_WARN("[WxInitInject] 初始化失败");
        MessageBox(NULL, L"初始化失败", L"WxInitInject", 0);
        return -1;
    }

    LOG_INFO("注入完成! 详细信息请见 spy.dll 输出日志");

    injected = true;
    return 0;
}

int WxDestroyInject()
{
    if (!injected) {
        return -1;
    }

    if (!CallDllFunc(wcProcess, spyDllPath, spyBase, "CleanupSpy", NULL)) {
        return -2;
    }

    if (!EjectDll(wcProcess, spyBase)) {
        return -3; // TODO: Unify error codes
    }

    LOG_INFO("[WxDestroyInject] 已取消注入并卸载 DLL");
    injected = false;
    return 0;
}

//所有的消息处理函数
INT_PTR CALLBACK DialogProc(HWND hwndDlg, UINT   uMsg, WPARAM wParam, LPARAM lParam)
{
    wchar_t title[99] = L"WeChat Injector for v";
    int i;
    switch (uMsg)
    {
    case WM_INITDIALOG:
        dlg = hwndDlg;
        wcscat_s(title, SUPPORT_VERSION);
        SetWindowTextW(hwndDlg, title);
        SetDlgItemTextA(hwndDlg, ID_DLL_NAME, "软件已启动");
        GetDllPath(spyDllPath);
        break;
        //按钮点击事件 处理
    case WM_COMMAND:
        if (wParam == INJECT_DLL && !injected)
        {
            WxInitInject(false);
            if (injected) {
                SetDlgItemTextA(hwndDlg, ID_DLL_NAME, "已注入到 WeChatWin.dll");
            }
        }
        if (wParam == UN_DLL && injected)
        {
            i = WxDestroyInject();
            if (i == 0) {
                SetDlgItemTextA(hwndDlg, ID_DLL_NAME, "已取消注入");
            }
            if (i == -1) {
                LOG_WARN("[WxDestroyInject] 当前没有被注入，无需取消注入");
            }
        }
        break;
    case WM_CLOSE:
        WxDestroyInject();
        EndDialog(hwndDlg, 0);
        break;
    default:
        break;
    }
    return FALSE;
}

//函数开始
int APIENTRY wWinMain(_In_ HINSTANCE hInstance,
    _In_opt_ HINSTANCE hPrevInstance,
    _In_ LPWSTR    lpCmdLine,
    _In_ int       nCmdShow)
{
    InitLogger("Sweet", std::filesystem::current_path().string() + "/logs/injector.log");

    DialogBox(hInstance, MAKEINTRESOURCE(ID_MAIN), NULL, &DialogProc);

    return 0;
}
