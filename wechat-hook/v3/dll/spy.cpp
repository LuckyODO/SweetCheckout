﻿#include <filesystem>

#include "log.h"
#include "spy.h"
#include "util.h"
#include "receive_msg.h"

UINT64 g_WeChatWinDllAddr = 0;

static bool IsWxVersionMatched(const wchar_t *version)
{
    if (wcscmp(version, SUPPORT_VERSION) != 0) {
        return false;
    }
    return true;
}

void InitSpy(LPVOID args)
{
    wchar_t version[16] = { 0 };
    PortPath_t *pp      = (PortPath_t *)args;
    string path = pp->path;
    InitLogger("Sweet", path + "/logs/spy.log");
    g_WeChatWinDllAddr = (UINT64)GetModuleHandle(L"WeChatWin.dll"); // 获取wechatWin模块地址
    if (g_WeChatWinDllAddr == 0) {
        LOG_ERROR("Failed to get address of module WeChatWin.dll");
        return; // TODO: 退出进程，避免后面操作失败
    }

    if (!GetWeChatVersion(version)) { // 获取微信版本
        LOG_ERROR("Failed to get WeChat version.");
        return;
    }
    LOG_INFO("WeChat version: {}", Wstring2String(version).c_str());
    if (!IsWxVersionMatched(version)) {
        LOG_ERROR("It is not supported by injector yet.");
        // MessageBox(NULL, L"不支持当前版本", L"错误", 0);
        return;
    }

    ListenMessage(pp);
    LOG_INFO("Inject successfully.");
}

void CleanupSpy()
{
    LOG_DEBUG("CleanupSpy");
    UnListenMessage();
    LOG_INFO("Eject successfully.");
}
