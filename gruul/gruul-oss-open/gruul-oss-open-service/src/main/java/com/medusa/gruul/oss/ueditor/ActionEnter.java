package com.medusa.gruul.oss.ueditor;


import com.medusa.gruul.oss.ueditor.define.ActionMap;
import com.medusa.gruul.oss.ueditor.define.AppInfo;
import com.medusa.gruul.oss.ueditor.define.BaseState;
import com.medusa.gruul.oss.ueditor.define.State;
import com.medusa.gruul.oss.ueditor.hunter.FileManager;
import com.medusa.gruul.oss.ueditor.hunter.ImageHunter;
import com.medusa.gruul.oss.ueditor.uploading.Uploader;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class ActionEnter {

    private HttpServletRequest request = null;

    private String rootPath = null;
    private String contextPath = null;

    private String actionType = null;

    private ConfigManager configManager = null;

    public ActionEnter(HttpServletRequest request, String rootPath) {
        this.request = request;
        this.rootPath = rootPath;
        this.actionType = request.getParameter("action");
        this.contextPath = request.getContextPath();
        this.configManager = ConfigManager.getInstance(this.rootPath, this.contextPath, request.getRequestURI());
    }

    public String exec() {
        String callbackName = this.request.getParameter("callback");

        if (callbackName != null) {

            if (!validCallbackName(callbackName)) {
                return new BaseState(false, AppInfo.ILLEGAL).toJSONString();
            }

            return callbackName + "(" + this.invoke() + ");";

        } else {
            System.out.println(this.invoke());
            return this.invoke();
        }

    }

    public String invoke() {

        if (actionType == null || !ActionMap.MAPPING.containsKey(actionType)) {
            return new BaseState(false, AppInfo.INVALID_ACTION).toJSONString();
        }

        if (this.configManager == null || !this.configManager.valid()) {
            return new BaseState(false, AppInfo.CONFIG_ERROR).toJSONString();
        }

        State state = null;

        int actionCode = ActionMap.getType(this.actionType);

        Map<String, Object> conf = null;

        switch (actionCode) {

            case ActionMap.CONFIG:
                return this.configManager.getAllConfig().toString();

            case ActionMap.UPLOAD_IMAGE:
            case ActionMap.UPLOAD_SCRAWL:
            case ActionMap.UPLOAD_VIDEO:
            case ActionMap.UPLOAD_FILE:
                conf = this.configManager.getConfig(actionCode);
                state = new Uploader(request, conf).doExec();
                break;

            case ActionMap.CATCH_IMAGE:
                conf = configManager.getConfig(actionCode);
                String[] list = this.request.getParameterValues((String) conf.get("fieldName"));
                state = new ImageHunter(conf).capture(list);
                break;

            case ActionMap.LIST_IMAGE:
            case ActionMap.LIST_FILE:
                conf = configManager.getConfig(actionCode);
                int start = this.getStartIndex();
                state = new FileManager(conf).listFile(start);
                break;
            default:

        }

        return state.toJSONString();

    }

    public int getStartIndex() {

        String start = this.request.getParameter("start");

        try {
            return Integer.parseInt(start);
        } catch (Exception e) {
            return 0;
        }

    }

    /**
     * callback参数验证
     */
    public boolean validCallbackName(String name) {

        if (name.matches("^[a-zA-Z_]+[\\w0-9_]*$")) {
            return true;
        }

        return false;

    }

}