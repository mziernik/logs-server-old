<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>About</title>
        <link rel="shortcut icon" href="img/ico.png"></link>
        <style>
            table td{
                padding: 4px 8px;
            }
            table th{
                padding-top: 8px;
                text-align: left;
            }
            table td:first-child {
                display: list-item;
                margin-left: 16px;
            }
            li{
                margin: 0;
                padding: 0;
            }
        </style>
    </head>
    <body style="font-family: Tahoma; font-size: 9pt">
        <a style="position: absolute; top: 4px; right: 8px" href="test">tester</a>
        <h3>Serwer logów</h3>
       
        <div>Aby dodać log należy wysłać poniższe parametry metodą POST na adres "/add", kodowanie "UTF-8"</div>

        <br/>
        <table>
            <tr>
                <th colspan="3" >Parametry wymagane:</th>
            </tr>
            <tr>
                <td>kind</td>
                <td>int [1..4]</td>
                <td>Rodzaj logu: 1: Zdarzenie, 2: Log, 3: Błąd, 4: Zapytanie</td>
            </tr>
            <tr>
                <td>source</td>
                <td>text [50]</td>
                <td>Źródło logu (nazwa aplikacji, usługi)</td>
            </tr>

            <tr>
                <td>type</td>
                <td>text [50]</td>
                <td>Typ/grupa logu</td>
            </tr>
            <tr>
                <td>date</td>
                <td>timestamp</td>
                <td>Data logu (klient), format "yyyy-MM-dd HH:mm:ss.SSS"</td>
            </tr>
            <tr>
                <th colspan="3">Parametry opcjonalne:</th>
            </tr>
            <tr>
                <td>number</td>
                <td>int</td>
                <td>Kolejny numer porządkowy wymagany do poprawnego wyświetlenia<br/>
                    kolejności logów, jeśli różnica będzie mniejsza niż 1ms</td>
            </tr>
            <tr>
                <td>address</td>
                <td>text [50]</td>
                <td>Adres lub nazwa źródłowego hosta</td>
            </tr>
            <tr>
                <td>device</td>
                <td>text [50]</td>
                <td>Nazwa urządzenia/komputera</td>
            </tr>
            <tr>
                <td>log</td>
                <td>text [1000]</td>
                <td>Treść logu, wartość podglądowa</td>
            </tr>
            <tr>
                <td>details</td>
                <td>text [10000]</td>
                <td>Szczegóły logu, zalecane dla większej ilości tekstu</td>
            </tr>
            <tr>
                <td>process</td>
                <td>int</td>
                <td>Identyfikator procesu</td>
            </tr>
            <tr>
                <td>thread</td>
                <td>int</td>
                <td>Identyfikator wątku</td>
            </tr>
            <tr>
                <td>session</td>
                <td>text [50]</td>
                <td>Identyfikator sesji serwera, instancji aplikacji</td>
            </tr>
            <tr>
                <td>request</td>
                <td>text [50]</td>
                <td>Identyfikator żądania serwera</td>
            </tr>
            <tr>
                <td>username</td>
                <td>text [50]</td>
                <td>Nazwa użytkownika</td>
            </tr>
            <tr>
                <td>expire</td>
                <td>int</td>
                <td>Czas wygaśnięcia logu (godziny), -1: wartość domyślna dla danego typu, 0: bez limitu</td>
            </tr>
            <tr>
                <td>extra</td>
                <td>text [1000]</td>
                <td>Dane dodatkowe</td>
            </tr>
        </table>
        <br/>
        <p> W przypadku gdy jeden z parametrów nie będzie zdefiniowany lub jego wartość nie będzie
            prawidłowa, serwer zwróci błąd HTTP 400. Przy prawidłowym żądaniu - 200 </p>


        <br/>
        <hr/>
        <div style="font-size: 8pt; color: #888">© Miłosz Ziernik</div>

    </body>
</html>
