package h2hbankgas.controller;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class TestSSLCCA {
	private static final String SSL_DIR = "C:\\ServcieAPIH2H\\CertTCB\\";
    private static final String API_LINK = "https://partner-apigw-uat.techcombank.com.vn/common-security-auth-services/v1/jwt-token";

    // Trust TCB
    private static final String CA_KEYSTORE_TYPE = KeyStore.getDefaultType();
    private static final String CA_KEYSTORE_PATH = SSL_DIR + "trust_local.jks";
    private static final String CA_KEYSTORE_PASS = "12345678";

    // Client Certificate Authentication
    private static final String CLIENT_KEYSTORE_TYPE = "PKCS12";
    private static final String CLIENT_KEYSTORE_PATH = SSL_DIR + "vnakeystore.p12";
    private static final String CLIENT_KEYSTORE_PASS = "12345678";

    public static String CallAPItoken() {
    	System.out.println("API LINK>>>>>>>>>>>>>>" + API_LINK);
    	String  ketqua ="";
        try (CloseableHttpClient httpClient = createHttpClient()) {
            HttpPost request = new HttpPost(API_LINK);

            // Set headers for the request
            request.setHeader("client_id", "98e5017a51f3429f93891c8786f18431");
            request.setHeader("client_secret", "093Aa2412EdC41d18c08D3f7eCf53481");
            request.setHeader("Content-Type", "application/json; charset=UTF-8");

            // Execute the request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                System.out.println(response.getStatusLine());

                if (entity != null) {
                    String responseString = EntityUtils.toString(entity, "UTF-8");
                    System.out.println("Response String:\n" + responseString);
                    ketqua = "Response String: " + responseString;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ketqua = "loi : " + e.getMessage();
        }
		return ketqua;
    }

    private static CloseableHttpClient createHttpClient() throws Exception {
        SSLContext sslContext = createSslCustomContext();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE
        );
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory())
                .register("https", sslsf)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(2000); // Max connections

        return HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(cm)
                .build();
    }

    private static SSLContext createSslCustomContext() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        // Trusted CA keystore
        KeyStore tks = KeyStore.getInstance(CA_KEYSTORE_TYPE);
        tks.load(new FileInputStream(CA_KEYSTORE_PATH), CA_KEYSTORE_PASS.toCharArray());

        // Client keystore
        KeyStore cks = KeyStore.getInstance(CLIENT_KEYSTORE_TYPE);
        cks.load(new FileInputStream(CLIENT_KEYSTORE_PATH), CLIENT_KEYSTORE_PASS.toCharArray());

        // Create a TrustManager that trusts all certificates
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return sslContext;
    }

}
