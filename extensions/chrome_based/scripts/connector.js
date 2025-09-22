'use strict';

let port = 9563;
let enabled = true;
let isAppRunning = true;

const updatePort = () => {
    chrome.storage.sync.get(["port"])
        .then((result) => {
            if (isObjectEmpty(result) || result === undefined) chrome.storage.sync.set({ port });
            else port = result.port;
        });
}

const updateEnable = async () => {
    await chrome.storage.local.get(["enabled"])
        .then(res => {
            if (isObjectEmpty(res) || res === undefined) chrome.storage.local.set({ enabled });
            else enabled = res.enabled;
        });
}

updatePort()
updateEnable()
const postLinks = async (data, isBatch) => {
    await updatePort()
    let URL_TO_POST = `http://localhost:${port}/single`;
    if (isBatch)
        URL_TO_POST = `http://localhost:${port}/batch`;
    fetch(URL_TO_POST, {
        method: 'POST',
        body: JSON.stringify(data),
        headers: {
            'Content-Type': 'application/json', // Only necessary headers
        }
    }).catch(async _ => {
        chrome.notifications.create('', {
            title: 'BitKip Extension',
            message: "Can't send url to BitKip, Is application running on the same port?",
            iconUrl: '../resources/icons/logo.png',
            type: 'basic'
        });
        isAppRunning = false;
        chrome.downloads.download({ url: data.url })
    });
}


const loadedContentScripts = {};
chrome.runtime.onMessage.addListener(async (message, sender, sendResponse) => {
    switch (message.type) {
        case "captcha":
            const tabId = sender.tab.id;
            loadedContentScripts[tabId] = false;
            sendResponse({ isOkToAddListener: true });
            break;
        case "storeFirstUrl":
            const tabId2 = sender.tab.id;
            if (!loadedContentScripts[tabId2]) {
                sendResponse({ isOkToAddListener: true });
                loadedContentScripts[tabId2] = true;
            } else sendResponse({ isOkToAddListener: false });
            break;
        case "downloadBatch":
            if (message.urls !== 0) {
                const links = message.urls;
                postLinks({ links }, true)
            }
            break;
        case "setPort":
            chrome.storage.sync.set({ port: message.value });
            break;
    }
});


// Prevent the download from starting
// get download link from Chrome Api
const triggerDownload = (downloadItem, suggest) => {
    if (isAppRunning){
        // final url is used when url itself is a redirecting link
        updateEnable();
        if (!enabled || (downloadItem.mime && downloadItem.mime.includes("image")))
            return;
        let url = downloadItem.finalUrl || downloadItem.url;
        if (isSupportedProtocol(url)) {
            suggest({ cancel: true, filename: downloadItem.filename });
            chrome.downloads.cancel(downloadItem.id, () =>
                chrome.downloads.erase({ id: downloadItem.id })
            );
            let data = {
                url,
                filename: downloadItem.filename,
                fileSize: downloadItem.fileSize,
                agent: navigator.userAgent
            };
            postLinks(data, false);
        }
    } else {
        suggest({filename: downloadItem.filename });
        isAppRunning = true;
    }
    
}

//Main code to maintain download link and start doing job
chrome.downloads.onDeterminingFilename.addListener(triggerDownload);

//Add BitKip right-click menu listener to browser page
chrome.contextMenus.onClicked.addListener((info) => {
    if (info.menuItemId === "extract_selected_link") {
        if (isSupportedProtocol(info.linkUrl)) {
            let data = {
                url: info.linkUrl,
                agent: navigator.userAgent
            };
            postLinks(data, false);
        }
    } else if (info.menuItemId === "settings")
        chrome.runtime.openOptionsPage();
});

//Adding menus to right-click
chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
        id: 'extract_selected_link',
        title: 'Download this link',
        contexts: ['all'],
    });
    chrome.contextMenus.create({
        id: 'settings',
        title: 'Settings',
        contexts: ['action']
    });
});


//check link to be valid or not
const isSupportedProtocol = (url) => {
    if (!url) return false;
    let u = new URL(url);
    return u.protocol === 'http:' || u.protocol === 'https:';
}


const isObjectEmpty = (objectName) => {
    return JSON.stringify(objectName) === "{}";
};

chrome.action.onClicked.addListener(() => {
    sendMessage({ type: 'toggleOverlay' });
});

const sendMessage = async (message) => {
    const tabs = await chrome.tabs.query({active: true, currentWindow: true});
    const success = await safeInject(tabs[0].id);
    
    if (success) {
        // Add a small delay to ensure content script is ready
        await new Promise(resolve => setTimeout(resolve, 100));
        await chrome.tabs.sendMessage(tabs[0].id, message);
    } else {
        chrome.notifications.create('', {
            title: 'BitKip Extension',
            message: "Can't open link selector. see logs",
            iconUrl: '../resources/icons/logo.png',
            type: 'basic'
        });
    }
}
const safeInject = async (tabId) => {
    // First check if already injected
    try {
        await chrome.tabs.sendMessage(tabId, {type: 'ping'});
        return true;
    } catch (e) {
        try {
            await chrome.scripting.executeScript({
                target: {tabId: tabId},
                files: ['./scripts/uSelect_overlay.js', './scripts/uSelect_statemachine.js']
            });
            return true;
        } catch (error) {
            console.error(error);
        }
    }
    return false;
}