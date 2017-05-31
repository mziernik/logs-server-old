

var api = new function() {
    this.callback;
    this.currentLog;

    this.addLog = function(log) {
        if (!addLog)
            return;

        log = JSON.parse(log);
        log.date = new Date(log.date);
        addLog(log);
    };

};

function notify(text) {
    api.callback.message(text);
}

function alert(text) {
    api.callback.alert(text);
}

function debug(text) {
    api.callback.debug(text);
}

function error(text) {
    api.callback.error(text);
}


