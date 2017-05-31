package cloud.mega;

import com.json.JArray;
import com.json.JObject;
import com.json.JSON;
import com.json.exceptions.JException;
import com.config.CProxy;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import com.mlogger.Log;

public class MegaHandler {

    private String email, password, sid;
    private int sequence_number;
    private long[] master_key;
    private BigInteger[] rsa_private_key;
    private long[] password_aes;
    HashMap<String, long[]> user_keys = new HashMap<String, long[]>();

    public MegaHandler(String email, String password) {
        this.email = email;
        this.password = password;
        Random rg = new Random();
        sequence_number = rg.nextInt(Integer.MAX_VALUE);
    }

    public int login() throws IOException {

        password_aes = MegaCrypt.prepare_key_pw(password);
        long time = System.currentTimeMillis();
        String uh = MegaCrypt.stringhash(email, password_aes);

        System.out.println("UH " + (System.currentTimeMillis() - time));

        JObject json = new JObject();
        json.put("a", "us");
        json.put("user", email);
        json.put("uh", uh);

        while (true) {
            String response = api_request(json.toString());

            if (isInteger(response))
                return Integer.parseInt(response);

            if (login_process(JObject.parse(response), password_aes) != -2) {
                break;
            }

        }

        return 0;
    }

    private int login_process(JObject json, long[] password_aes) throws IOException {

        String master_key_b64 = null;

        master_key_b64 = json.getStr("k");

        if (master_key_b64 == null || master_key_b64.isEmpty())
            return -1;

        long[] encrypted_master_key = MegaCrypt.base64_to_a32(master_key_b64);
        master_key = MegaCrypt.decrypt_key(encrypted_master_key, password_aes);

        if (json.has("csid")) {
            String encrypted_rsa_private_key_b64 = null;

            encrypted_rsa_private_key_b64 = json.getStr("privk");

            long[] encrypted_rsa_private_key = MegaCrypt.base64_to_a32(encrypted_rsa_private_key_b64);
            long[] rsa_private_key = MegaCrypt.decrypt_key(encrypted_rsa_private_key, master_key);
            String private_key = MegaCrypt.a32_to_str(rsa_private_key);

            this.rsa_private_key = new BigInteger[4];
            for (int i = 0; i < 4; i++) {
                int l = ((((int) private_key.charAt(0)) * 256 + ((int) private_key.charAt(1)) + 7) / 8) + 2;
                this.rsa_private_key[i] = MegaCrypt.mpi_to_int(private_key.substring(0, l));
                private_key = private_key.substring(l);
            }

            BigInteger encrypted_sid = null;

            encrypted_sid = MegaCrypt.mpi_to_int(MegaCrypt.base64_url_decode(json.getStr("csid")));

            BigInteger modulus = this.rsa_private_key[0].multiply(this.rsa_private_key[1]);
            BigInteger privateExponent = this.rsa_private_key[2];

            BigInteger sid = null;
            try {
                PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, privateExponent));
                Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                // PyCrypt can handle >256 bit length... what the fuck... sometimes i get 257
                if (encrypted_sid.toByteArray().length > 256) {
                    Random rg = new Random();
                    sequence_number = rg.nextInt(Integer.MAX_VALUE);
                    return -2;  // lets get a new seession
                }
                sid = new BigInteger(cipher.doFinal(encrypted_sid.toByteArray()));
            } catch (Exception e) {
                Log.warning(e);
                return -1;
            }

            String sidS = sid.toString(16);
            if (sidS.length() % 2 != 0)
                sidS = "0" + sidS;
            try {
                byte[] sidsnohex = MegaCrypt.decodeHexString(sidS);
                this.sid = MegaCrypt.base64_url_encode(new String(sidsnohex, "ISO-8859-1").substring(0, 43));
            } catch (Exception e) {
                Log.warning(e);
                return -1;
            }
        }
        return 0;
    }

    public String add_user(String email) throws IOException {
        JObject json = new JObject();
        json.put("a", "ur");
        json.put("u", email);
        json.put("l", 1);
        return api_request(json.toString());
    }

    public long get_quota() throws IOException {
        JObject json = new JObject();
        json.put("a", "uq");
        json.put("xfer", 1);
        return JObject.parse(api_request(json.toString())).getNumber("mstrg", 0l).longValue();
    }

    public String get_user() throws IOException {
        JObject json = new JObject();
        json.put("a", "ug");
        return api_request(json.toString());
    }

    public ArrayList<MegaFile> get_files() throws JException, IOException {
        JObject json = new JObject();
        json.put("a", "f");
        json.put("c", "1");

        String files = api_request(json.toString());
        // TODO check for negativ error
        //print(json.toString());
        ArrayList<MegaFile> megaFiles = new ArrayList<MegaFile>();

        JArray array = null;

        json = JObject.parse(files);
        array = json.arrayF("f");

        for (JObject ob : array.getObjects())
            megaFiles.add(process_file(ob));

        return megaFiles;
    }

    private MegaFile process_file(JObject jsonFile) throws JException, UnsupportedEncodingException {

        MegaFile file = new MegaFile();

        if (jsonFile.getInt("t") < 2) {

            String key = "";
            String uid = jsonFile.getStr("u");
            String h = (jsonFile.getStr("h"));
            file.setUID(uid);
            file.setHandle(h);
            //print (h);
            if (jsonFile.getStr("k").contains("/")) {
                String[] keys = jsonFile.getStr("k").split("/");
                int start = keys[0].indexOf(":") + 1;
                key = keys[0].substring(start);

            }

            String attributes = MegaCrypt.base64_url_decode(jsonFile.getStr("a"));

            long[] k = new long[4];
            if (!key.isEmpty()) {
                long[] keys_a32 = MegaCrypt.decrypt_key(MegaCrypt.base64_to_a32(key), master_key);
                if (jsonFile.getInt("t") == 0) {

                    k[0] = keys_a32[0] ^ keys_a32[4];
                    k[1] = keys_a32[1] ^ keys_a32[5];
                    k[2] = keys_a32[2] ^ keys_a32[6];
                    k[3] = keys_a32[3] ^ keys_a32[7];

                }
                else {
                    k[0] = keys_a32[0];
                    k[1] = keys_a32[1];
                    k[2] = keys_a32[2];
                    k[3] = keys_a32[3];
                    file.setDirectory(true);

                }

                file.setKey(k);
                file.setAttributes(MegaCrypt.decrypt_attr(attributes, k));
            }
            else
                if (!jsonFile.has("su") && !jsonFile.has("sk") && jsonFile.getStr("k").contains(":")) {
                    long[] keyS;

                    user_keys.put(jsonFile.getStr("u"), MegaCrypt.decrypt_key(MegaCrypt.base64_to_a32(jsonFile.getStr("sk")), master_key));
                    //print("ShareKey->"+jsonFile.getStr("sk"));
                    int dd1 = jsonFile.getStr("k").indexOf(':');
                    String sk = jsonFile.getStr("k").substring(dd1 + 1);

                    keyS = MegaCrypt.decrypt_key(MegaCrypt.base64_to_a32(sk), user_keys.get(jsonFile.getStr("u")));
                    if (jsonFile.getInt("t") == 0) {
                        long[] keys_a32S = keyS;
                        k[0] = keys_a32S[0] ^ keys_a32S[4];
                        k[1] = keys_a32S[1] ^ keys_a32S[5];
                        k[2] = keys_a32S[2] ^ keys_a32S[6];
                        k[3] = keys_a32S[3] ^ keys_a32S[7];
                    }
                    else {
                        k = keyS;
                        file.setDirectory(true);
                    }

                    file.setKey(k);
                    file.setAttributes(MegaCrypt.decrypt_attr(attributes, k));

                }
                else
                    if (!jsonFile.has("u") && jsonFile.getStr("k").contains(":") && user_keys.containsKey(jsonFile.getStr("u"))) {

                        int dd1 = jsonFile.getStr("k").indexOf(':');
                        String sk = jsonFile.getStr("k").substring(dd1 + 1);
                        //print(user_keys.get(jsonFile.getStr("u")));
                        long[] keyS = MegaCrypt.decrypt_key(MegaCrypt.base64_to_a32(sk), user_keys.get(jsonFile.getStr("u")));
                        if (jsonFile.getInt("t") == 0) {
                            long[] keys_a32S = keyS;
                            k[0] = keys_a32S[0] ^ keys_a32S[4];
                            k[1] = keys_a32S[1] ^ keys_a32S[5];
                            k[2] = keys_a32S[2] ^ keys_a32S[6];
                            k[3] = keys_a32S[3] ^ keys_a32S[7];
                        }
                        else {
                            k = keyS;
                            file.setDirectory(true);
                        }

                        file.setKey(k);
                        file.setAttributes(MegaCrypt.decrypt_attr(attributes, k));

                    }
                    else
                        if (!jsonFile.has("k")) {
                            int dd1 = jsonFile.getStr("k").indexOf(':');
                            key = jsonFile.getStr("k").substring(dd1 + 1);
                            long[] keys_a32S = MegaCrypt.decrypt_key(MegaCrypt.base64_to_a32(key), master_key);
                            if (jsonFile.getInt("t") == 0) {

                                k[0] = keys_a32S[0] ^ keys_a32S[4];
                                k[1] = keys_a32S[1] ^ keys_a32S[5];
                                k[2] = keys_a32S[2] ^ keys_a32S[6];
                                k[3] = keys_a32S[3] ^ keys_a32S[7];
                                file.setDirectory(true);

                            }/*else{
                             k = keys_a32S;

                             file.setDirectory(true);

                             }*/

                            file.setKey(k);

                            file.setAttributes(MegaCrypt.decrypt_attr(attributes, k));
                        }
                        else {
                            file.setAttributes(jsonFile.toString());
                        }

        }
        else
            if (jsonFile.getInt("t") == 2) {
                file.setName("Cloud Drive");
            }
            else
                if (jsonFile.getInt("t") == 3) {
                    file.setName("Cloud Inbox");
                }
                else
                    if (jsonFile.getInt("t") == 4) {
                        file.setName("Rubbish Bin");
                    }
                    else {
                        file.setName(jsonFile.toString());
                    }
        return file;

    }

    public String get_url(MegaFile f) throws IOException {

        if (f.getHandle() == null || f.getKey() == null)
            return "Error";
        JObject json = new JObject();
        json.put("a", "l");
        json.put("n", f.getHandle());

        String public_handle = api_request(json.toString());
        if (public_handle.equals("-11"))
            return "Shared file, no public url";
        return "https://mega.co.nz/#!" + public_handle.substring(1, public_handle.length() - 1) + "!" + MegaCrypt.a32_to_base64(f.getKey());

    }

    private String api_request(String data) throws IOException {
        HttpURLConnection connection = null;

        String urlString = "https://g.api.mega.co.nz/cs?id=" + sequence_number;
        if (sid != null)
            urlString += "&sid=" + sid;

        URL url = new URL(urlString);
        connection = (HttpURLConnection) url.openConnection(CProxy.getProxy(url));
        connection.setRequestMethod("POST"); //use post method
        connection.setDoOutput(true); //we will send stuff
        connection.setDoInput(true); //we want feedback
        connection.setUseCaches(false); //no caches
        connection.setAllowUserInteraction(false);
        connection.setRequestProperty("Content-Type", "text/xml");

        OutputStream out = connection.getOutputStream();
        try {
            OutputStreamWriter wr = new OutputStreamWriter(out);
            wr.write("[" + data + "]"); //data is JSON object containing the api commands
            wr.flush();
            wr.close();
        } finally { //in this case, we are ensured to close the output stream
            if (out != null)
                out.close();
        }

        InputStream in = connection.getInputStream();
        StringBuffer response = new StringBuffer();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            rd.close(); //close the reader

        } finally {  //in this case, we are ensured to close the input stream
            if (in != null)
                in.close();
        }

        return response.toString().substring(1, response.toString().length() - 1);

    }

    public static boolean isInteger(String string) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        int length = string.length();
        int i = 0;
        if (string.charAt(i) == '[') {
            if (length == 1)
                return false;
            i++;
        }
        if (string.charAt(i) == '-') {
            if (length == 1 + i)
                return false;
            i++;
        }
        for (; i < length; i++) {
            char c = string.charAt(i);
            if (c <= '/' || c >= ':') {
                return false;
            }
        }
        return true;
    }

    public void download(String url, String path) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException, IllegalBlockSizeException, BadPaddingException {
        print("Download started");
        String[] s = url.split("!");
        String file_id = s[1];
        byte[] file_key = MegaCrypt.base64_url_decode_byte(s[2]);

        int[] intKey = MegaCrypt.aByte_to_aInt(file_key);
        JObject json = new JObject();
        json.put("a", "g");
        json.put("g", "1");
        json.put("p", file_id);

        JObject file_data = JObject.parse(api_request(json.toString()));
        int[] keyNOnce = new int[]{intKey[0] ^ intKey[4], intKey[1] ^ intKey[5], intKey[2] ^ intKey[6], intKey[3] ^ intKey[7], intKey[4], intKey[5]};
        byte[] key = MegaCrypt.aInt_to_aByte(keyNOnce[0], keyNOnce[1], keyNOnce[2], keyNOnce[3]);

        int[] iiv = new int[]{keyNOnce[4], keyNOnce[5], 0, 0};
        byte[] iv = MegaCrypt.aInt_to_aByte(iiv);

        @SuppressWarnings("unused")
        int file_size = file_data.getInt("s");
        String attribs = (file_data.getStr("at"));
        attribs = new String(MegaCrypt.aes_cbc_decrypt(MegaCrypt.base64_url_decode_byte(attribs), key));

        String file_name = attribs.substring(10, attribs.lastIndexOf("\""));
        print(file_name);
        final IvParameterSpec ivSpec = new IvParameterSpec(iv);
        final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CTR/nopadding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
        InputStream is = null;
        String file_url = file_data.getStr("g");

        FileOutputStream fos = new FileOutputStream(path + File.separator + file_name);
        final OutputStream cos = new CipherOutputStream(fos, cipher);
        final Cipher decipher = Cipher.getInstance("AES/CTR/NoPadding");
        decipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
        int read = 0;
        final byte[] buffer = new byte[32767];
        try {

            URLConnection urlConn = new URL(file_url).openConnection();

            print(file_url);
            is = urlConn.getInputStream();
            while ((read = is.read(buffer)) > 0) {
                cos.write(buffer, 0, read);
            }
        } finally {
            try {
                cos.close();
                if (is != null) {
                    is.close();
                }
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }
        print("Download finished");
    }

    public static void print(Object o) {
        System.out.println(o);
    }
}
