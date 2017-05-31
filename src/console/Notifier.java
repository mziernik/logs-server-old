package console;

import com.utils.collections.HashList;
import java.util.Collection;
import logs.TLog;

public class Notifier implements Cloneable {
    
    public final HashList<TLog> added = new HashList<>();
    public final HashList<TLog> removed = new HashList<>();
    public String alert;
    public String error;
    public boolean statusesChanged = false;

    //--------------------------------
    public long lastNotified;
    private final Console console;
    
    public Notifier(Console console) {
        this.console = console;
    }
    
    private void doNotify() {
        synchronized (this) {
            lastNotified = System.currentTimeMillis();
            this.notifyAll();
        }
    }
    
    public static void statusesChanged() {
        for (Console cs : Console.allSockets)
            cs.notifier.statusesChanged = true;
    }
    
    public static void addAll(Collection<TLog> logs) {
        for (Console cs : Console.allSockets)
            synchronized (cs.notifier) {
                cs.notifier.added.addAll(logs);
                cs.notifier.doNotify();
            }
        
    }
    
    public static void add(TLog log) {
        for (Console cs : Console.allSockets)
            synchronized (cs.notifier) {
                cs.notifier.added.add(log);
                cs.notifier.doNotify();
            }
    }
    
    public static void add(Collection<TLog> logs) {
        
     //   for (TLog l : logs)
     //       com.Console.printlnTs("ID: %d, Notifier", l.id);
        
        for (Console cs : Console.allSockets)
            synchronized (cs.notifier) {
                cs.notifier.added.addAll(logs);
                cs.notifier.doNotify();
            }
    }
    
    public static void alert(String userName, String alert) {
        for (Console cs : Console.allSockets)
            if (userName == null || userName.equals(cs.getUserName())) {
                cs.notifier.alert = alert;
                cs.notifier.doNotify();
            }
        
    }
    
    public static void error(String userName, String error) {
        for (Console cs : Console.allSockets)
            if (userName == null || userName.equals(cs.getUserName()))
                synchronized (cs) {
                    cs.notifier.error = error;
                    cs.notifier.doNotify();
                }
    }
    
    public static void removeAll(Collection<TLog> logs) {
        for (Console cs : Console.allSockets)
            synchronized (cs.notifier) {
                cs.notifier.removed.addAll(logs);
                cs.notifier.doNotify();
            }
    }
    
    @Override
    public Notifier clone() throws CloneNotSupportedException {
        super.clone();
        
        Notifier notifier = new Notifier(console);
        
        synchronized (this) {
            notifier.statusesChanged = statusesChanged;
            notifier.alert = alert;
            notifier.error = error;
            notifier.added.addAll(added);
            notifier.removed.removeAll(removed);
            added.clear();
            removed.clear();
            alert = null;
            error = null;
            statusesChanged = false;
        }
        return notifier;
    }
    
    boolean isEmpty() {
        return added.isEmpty()
                && removed.isEmpty()
                && alert == null
                && error == null
                && !statusesChanged;
    }
    
}
