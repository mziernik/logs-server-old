package storage;

import com.config.CLogs;
import com.config.engine.interfaces.Cfg;
import com.config.engine.items.*;

@Cfg(name = "Magazyn lokalny")
public class CLogsStorage {

    @Cfg(name = "Aktywne")
    public final static CBool enabled = new CBool("service.logs.storage.enabled", true);

    @Cfg(name = "Ścieżka", nulls = true)
    public final static CString path = new CString("service.logs.storage.path", null);

    @Cfg(name = "Maksymalny rozmiar pliku", unit = "MB")
    public final static CInt maxFileSize = new CInt("service.logs.storage.max_file_size", 300);

    @Cfg(name = "Maksymalny czas wstrzymania", unit = "ms", desc = "Większy czas zmniejsza ilosść "
            + "operacji zapisu dyskowego, kosztem większego zużycia pamięci (buforowanie)")
    public final static CInt suspendTime = new CInt("service.logs.storage.suspend_time", 300);

    @Cfg(name = "Maksymalny rozmiar bufora pamięci", unit = "MB")
    public final static CInt memoryBuferLimit = new CInt("service.logs.storage.memory_bufer_limit", 10);

    @Cfg(name = "Zakres dni", desc = "Ilość dni, z których logi zapisane będą w jednym pliku")
    public final static CInt daysRange = new CInt("service.logs.storage.days_range", 300);

    @Cfg(name = "Maksymalna ilość wyświetlanych logów", min = 10, max = 10000)
    public final static CInt pageLogsLimit = new CInt("service.logs.storage.page_logs_limit", 300);

}
