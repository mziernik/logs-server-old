package logs;

import com.mlogger.statusses.Status;
import com.mlogger.statusses.StatusItem;
import console.*;
import java.util.*;
import service.Config.CConsole;

/**
 * Miłosz Ziernik 2013/06/20
 */
public final class Statuses {

    /**
     *
     */
    public final static Map<String, TStatus> all = new HashMap<>();

    public static void add(TStatus log) {

        synchronized (all) {

            //   String name = log.source + (log.)
            TStatus stat = all.get(log.key);
            if (stat == null) {
                all.put(log.key, log);
                stat = log;
            }

            // łączenie obiektow
            new Object() {

                void visit(TStatus src, TStatus dst) {

                    for (TStatus st : src.children.values()) {

                        TStatus stat = dst.children.get(st.key);

                        if (stat == null)
                            stat = new TStatus(dst, st.key, st.caption);

                        stat.comment = st.comment;
                        stat.caption = st.caption;
                        visit(st, stat);
                    }

                    for (TStatusItem it : src.items.values())
                        it.move(dst);

                }
            }.visit(log, stat);
        }
        Notifier.statusesChanged();
    }

    public static class TStatusItem extends StatusItem {

        public final long created = System.currentTimeMillis();

        public TStatusItem(TStatus parent, String key) {
            super(null, key);
            parent.items.put(key, this);
            parent.updated = System.currentTimeMillis();
        }

        public void move(TStatus dst) {
            dst.items.put(key, this);
            parent = dst;
            dst.updated = System.currentTimeMillis();
        }

        public long left() {
            Integer alive = CConsole.statusAliveTime.value(60);
            int left = expire == null ? alive : expire;
            if (left <= 0)
                left = alive;
            return ((created + left * 1000) - System.currentTimeMillis()) / 1000;
        }
    }

    public static class TStatus extends Status {

        public final Map<String, TStatus> children = new LinkedHashMap<>();
        public final Map<String, TStatusItem> items = new LinkedHashMap<>();
        private long updated = System.currentTimeMillis();
        //   public long created = System.currentTimeMillis();

        public TStatus(TStatus parent, String key, String caption) {
            super(null, key, caption);
            if (parent != null)
                parent.children.put(key, this);
        }

        public long left() {
            Integer alive = CConsole.statusAliveTime.value(60);
            int left = expire == null ? alive : expire;
            if (left <= 0)
                left = alive;
            return ((updated + left * 1000) - System.currentTimeMillis()) / 1000;
        }

        /*    public long left() {
         Integer alive = CConsole.statusAliveTime.value(60);
         int left = expire == null ? alive : expire;
         if (left <= 0)
         left = alive;
         return ((created + left * 1000) - System.currentTimeMillis()) / 1000;
         }
         */
        // czy cos zostalo usuniete
        boolean cleanup() {

            return new Object() {

                private boolean visit(TStatus status) {

                    boolean state = false;

                    Set<String> toRemove = new HashSet<>();
                    for (TStatus st : status.children.values()) {
                        state |= visit(st);

                        if (st.children.isEmpty() && st.items.isEmpty() && st.left() < 0)
                            toRemove.add(st.key);
                    }

                    state |= !toRemove.isEmpty();
                    for (String s : toRemove)
                        status.children.remove(s);

                    toRemove.clear();

                    for (TStatusItem item : status.items.values())
                        if (item.left() < 0)
                            toRemove.add(item.key);

                    state |= !toRemove.isEmpty();
                    for (String s : toRemove)
                        status.items.remove(s);

                    return state;
                }

            }.visit(this);
        }

    }
}
