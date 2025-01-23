'use strict';

let port = 9563;
let enabled = true;


const updatePort = () => {
    browser.storage.sync.get(["port"])
        .then((result) => {
            if (isObjectEmpty(result) || result === undefined) browser.storage.sync.set({port});
            else port = result.port;
        });
}

const updateEnable = async () => {
    await browser.storage.local.get(["enabled"])
        .then(res => {
            if (isObjectEmpty(res) || res === undefined) browser.storage.local.set({enabled});
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
        browser.notifications.create('', {
            title: 'BitKip Extension',
            message: "Can't send url to BitKip, Is application running on the same port?",
            iconUrl: '../resources/icons/logo.png',
            type: 'basic'
        });
        browser.downloads.onCreated.removeListener(triggerDownload);
        await browser.downloads.download({url: data.url});
        browser.downloads.onCreated.addListener(triggerDownload);
    });
}


const loadedContentScripts = {};
browser.runtime.onMessage.addListener(async (message, sender, sendResponse) => {
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
            browser.storage.sync.set({port: message.value});
            break;
    }
});


// Prevent the download from starting
// get download link from Firefox Api
const triggerDownload = (downloadItem, suggest) => {
    // final url is used when url itself is a redirecting link
    updateEnable();
    if (!enabled || (downloadItem.mime && downloadItem.mime.includes("image")))
        return;
    let url = downloadItem.finalUrl || downloadItem.url;
    if (isSupportedProtocol(url)) {
        let filename = downloadItem.filename;
        if (filename.includes('/')) {
            const start = downloadItem.filename.lastIndexOf('/');
            filename = downloadItem.filename.substring(start === -1 ? 0 : start + 1);
        }
        let data = {
            url,
            filename,
            fileSize: downloadItem.fileSize,
            agent: navigator.userAgent
        };
        browser.downloads.cancel(downloadItem.id);
        postLinks(data, false);
    }
}

//Main code to maintain download link and start doing job
browser.downloads.onCreated.addListener(triggerDownload);

//Add BitKip right-click menu listener to browser page
browser.contextMenus.onClicked.addListener((info) => {
    if (info.menuItemId === "extract_selected_link") {
        if (isSupportedProtocol(info.linkUrl)) {
            let data = {
                url: info.linkUrl,
                agent: navigator.userAgent
            };
            postLinks(data, false);
        }
    }
});

//Adding menus to right-click
browser.contextMenus.removeAll(() => {
    browser.contextMenus.create({
        id: 'extract_selected_link',
        title: 'Download this link',
        contexts: ['all'],
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

let isScriptInjected = false;

browser.action.onClicked.addListener(async (tab) => {
    if (!isScriptInjected) {
    
        browser.scripting.executeScript({
            target: { tabId: tab.id },
            files: ['scripts/uSelect_statemachine.js']
        }).then(() => {
            return browser.scripting.executeScript({
                target: { tabId: tab.id },
                files: ['scripts/uSelect_overlay.js'],
            });
        }).then(async () => {
            const tabs = await browser.tabs.query({ active: true, currentWindow: true, lastFocusedWindow: true });
            browser.tabs.sendMessage(tabs[0].id, { type: 'toggleOverlay' });
        }).catch((error) => {
            console.error('Failed to inject scripts:', error);
        });
        isScriptInjected = true;
    } else {
        const tabs = await browser.tabs.query({ active: true, currentWindow: true, lastFocusedWindow: true });
        browser.tabs.sendMessage(tabs[0].id, { type: 'toggleOverlay' });
    }
});
