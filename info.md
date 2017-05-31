##Szkielet projektu bazującego na frameworku.

[TOC]

####Konfiguracja środowiska

**Propertes:**

- Libraries:
 - Build Projects on Classpath: false
- Build -> Compiling
 - Compile in Save: true (automatyczna kompilacja / wstrzykiwanie źródeł)
- Run:
  - Working Directory: **bin**
  - VM Options: 
   - **-DDevMode=true** *(tryb deweloperski)* 
   - **-Dresources.path=..\web** *(mapowanie katalogu zasobów - aby wskazywał na źródłowy katalog)*
   - **-Dsources.path=..\src** *(katalog plików źródłowych)*

>Aby zadziałało prawidłowo wstrzykiwanie, należy aktywować opcję Tools -> Options -> Java -> Java debugger -> Apply code changes after save
  
###Struktura katalogów:
- **src**: źródła projektu
- **lib**: biblioteki 
 - **lib/embedded**: biblioreki dla wersji wbudowanej
- **test**: testy jednostkowe
- **web**: katalog plików zasobów
 - **web/WEB-INF**: zasoby wewnętrzne (nie dostępne prze URL)
 - **web/META-IN**F: katalog meta danych. Nie należy tutaj dodawać plików
- **bin**: katalog wynikowy binarek
- **res**: katalog zasobów, które będą automatycznie kopiowane do katalogu bin po zbudowaniu projektu
- **backup**: automatyczne kopie zapasowe
  
  
  ###Struktura projektu
  - cała wynikowa struktura plików w katalogu bin,
  - tryb aplikacji web-owej, serwer wbudowany lub jako serwlet,
  - jednoczesne budowanie plików WAR oraz JAR,
  - automatyczne kopie zapasowe plików źródłowych, zasobów, oraz struktury projektu (katalog backup)
  - automatyczne wersjnowanie - plik web/WEB-INF/version.properties,
  - automatyczne tworzenie skryptów uruchamiajacyh plik jar (bat oraz sh),
  - generowanie pliku wstępnej konfiguracji (pre_config.json),
  - archiwizacja źródeł projektu w pliku zip (na potrzeby mechanimów obsługi błędów),


###Struktura katalogów docelowych
  - bin: katalog główny
  - bin/appdata: pliki konfiguracyjnych usługi,
  - bin/appdata/users: pliki użytkowników (json),
  - bin/lib: biblioteki,
  - bin/temp: katalog tymczasowy,
  - bin/logs: logi
  - web: zasoby (webapp)
  - web/WEB-INF: zasoby nie dostępne z zewnątrz

####Skrypt ANT-a
Aby skrypt działał poprawnie należy umieścić plik framework.xml w katalogu nbproject, oraz dokonać modyfikacji pliku build.xml:

**build.xml**

```xml
<project name="Logi" default="default" basedir=".">
  
    <property name="jar.name" value="content.jar"/>
    <property name="war.name" value="ROOT.war"/>
    <property name="skip.war" value=""/>
    <!--<property name="skip.pack.sources" value=""/>-->
    <!--<property name="skip.backup" value=""/>-->
    <!--<property name="skip.run-script" value=""/>-->
    
    <import file="nbproject/framework.xml"/> 
    <import file="nbproject/build-impl.xml"/>	
        
    <target name="-pre-init" depends="fra-pre-init"> </target>
    <target name="-post-init" depends="fra-post-init"> </target>    
    <target name="-pre-compile" depends="fra-pre-compile"> </target>   
    <target name="-post-compile" depends="fra-post-compile"> 
<!--        <echo level="info">  Kopiowanie biblioteki Framework.jar  </echo>
        <copy file = "..\Framework\dist\Framework.jar" tofile = "lib\Framework.jar" />-->
    </target>
    <target name="-post-clean" depends="fra-post-clean"> </target>    
    <target name="-post-jar" depends="fra-post-jar"> </target>		
	
</project>
```
Kolejność deklaracji:
1. `<property/>`
2.  `<import file="nbproject/framework.xml"/> `
3. `<import file="nbproject/build-impl.xml"/>`  
4. `<target/>`

Deklarownie we właściwej kolejności zagwarantuje prawidłowe nadpisanie właściwoścu oraz target-ów. Predefioniowane targety należy przeciążyć tak:
```xml
<target name="-pre-init" depends="fra-pre-init"/>
<target name="-post-init" depends="fra-post-init"/>    
<target name="-pre-compile" depends="fra-pre-compile"/>   
<target name="-post-compile" depends="fra-post-compile"/>
<target name="-post-clean" depends="fra-post-clean"/>    
<target name="-post-jar" depends="fra-post-jar"/>	
```
W każdej sekcji można dopisać własne instrukcje. Zachowanie atrybutu `depends` zapewni wcześniejsze wykonanie danego etapu. Można również użyć `<antcall target="..."/>```language
```

#####Właściwości
- `bin.dir = bin` katalog binarek
- `jar.name = ${ant.project.name}.jar` nazwa pliku JAR
- `war.name = ${ant.project.name}.war` nazwa pliku WA
- `src.dir = src` katalog źródłowy klas javy
- `web.dir = web` katalog źródłowy zasobów web
- `backup.dir = backup` katalog backupu plikó źródłowych oraz web
- `res.dir = res` katalog docelowy struktury web
- `build.web.dir = ${bin.dir}/web` docelowy katalog web

Skrypty:
- `cleanup.script.name = cleanup` nazwa skryptu czyszczącego zbędne pliki (bez rozszerzenia)
- `run.script.name = run` nazwa skryptu uruchamiania .bat oraz .sh (bez rozszerzenia)
- `run.script.bat.before = @echo off` skrypt wykonywany przed wywołeniem procesu
- `run.script.sh.before =`
- `run.script.bat.after = timeout /T 2`skrypt wykonywany po wywołeniem procesu
- `run.script.sh.after =`
- `run.script.java.args =` argumenty maszynny wirtualnej

Stałe:
- `work.dir = ${bin.dir}`
- `build.dir = ${bin.dir}/temp/build`
- `dist.dir = ${build.dir}/dist`
- `build.classes.dir = ${build.web.dir}/WEB-INF/classes`
- `dist.jar = ${bin.dir}/${jar.name}`
- `dist.war = ${bin.dir}/${war.name}`

Wyłączanie funckcjonalności:
- `skip.war` nie buduj pliku WAR
- `skip.pack.sourcesr` nie archiwizuj źródeł
- `skip.backup` nie wykonuj kopii zapasowej
- `skip.run-script` nie twórz skryptów uruchamiających usługę


