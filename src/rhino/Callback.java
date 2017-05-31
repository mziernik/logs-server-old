package rhino;

import console.Notifier;

public class Callback {

    private final Scripts scripts;

    public Callback(Scripts scripts) {
        this.scripts = scripts;
    }

    public void alert(String message) {
        Notifier.alert(scripts.userName, message);
    }

    public void error(String message) {
        Notifier.error(scripts.userName, message);
    }

    public void debug(String message) {
        //   mlogger.Log.debug("JS", message);
    }
}
