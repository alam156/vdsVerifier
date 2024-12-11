package com.example.myapplication;

import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pmw.tinylog.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.tsenger.vdstools.Verifier;
import de.tsenger.vdstools.vds.DigitalSeal;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private Button scanQrBtn;
    private TextView scannedValueTv;
    private TextView expiryDateValueTv;

    private TextView datOfBirthValueTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        scanQrBtn = findViewById(R.id.scanQrBtn);
        scannedValueTv = findViewById(R.id.scannedValueTv);
        expiryDateValueTv = findViewById(R.id.expiryDateValueTv);
        datOfBirthValueTv = findViewById(R.id.datOfBirthValueTv);

        registerUiListener();
    }
    private void registerUiListener() {
        scanQrBtn.setOnClickListener(v ->
                scannerLauncher.launch(
                        new ScanOptions()
                                .setPrompt("Scan Qr Code")
                                .setDesiredBarcodeFormats(ScanOptions.DATA_MATRIX)
                )
        );
    }

    private final ActivityResultLauncher<ScanOptions> scannerLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
//                X509Certificate cert =null;
//                String keyStorePassword = "vdstools";
//                KeyStore ks = null;
//                Security.removeProvider("BC");
//                Security.addProvider(new BouncyCastleProvider());
//                KeyStore keyStore = null;
//
//                try {
//                    InputStream caInput = getResources().openRawResource(R.raw.vdstools_testcerts);
//                    ks = getKeystore(caInput,keyStorePassword);
//                    cert = (X509Certificate) ks.getCertificate("dets32");
//                }  catch (KeyStoreException e) {
//                    throw new RuntimeException(e);
//                }
//
//                if (result.getContents() == null) {
//                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
//                } else {
//                    Logger.debug("my log",result.getContents());
//                    DigitalSeal digitalSealToVerify = DigitalSeal.fromRawString(result.getContents());
//                    String signerCertRef = digitalSealToVerify.getSignerCertRef();
//                    try {
//                        InputStream is = getResources().openRawResource(R.raw.sealgen_dets32);
//                        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
//                        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(is);
//                        is.close();
//                        Verifier verifier = new Verifier(digitalSealToVerify, certificate);
//                        Verifier.Result result1 = verifier.verify();
//                        Logger.debug(result1.toString());
//                        scannedValueTv.setText("Scanned Value : " + result1.toString());
//                    }  catch (CertificateException e) {
//                        throw new RuntimeException(e);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    } catch (NoSuchProviderException e) {
//                        throw new RuntimeException(e);
//                    }
                String password_ = "bccca";
                FileInputStream fis = null;
                X509Certificate cert =null;
               Security.removeProvider("BC");
               Security.addProvider(new BouncyCastleProvider());
                try {
                    InputStream rawInputStream = getResources().openRawResource(R.raw.ahad_cert);
                    fis = convertInputStreamToFileInputStream(rawInputStream);
//                    CertificateFactory factory = CertificateFactory.getInstance("X.509", "BC");
//                    cert = (X509Certificate) factory.generateCertificate(fis);
                    KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
                    keyStore.load(fis, password_.toCharArray());
                    Enumeration<String> aliases = keyStore.aliases();
                    while (aliases.hasMoreElements()) {
                        String alias = aliases.nextElement();
                        cert = (X509Certificate) keyStore.getCertificate(alias);
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (CertificateException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchProviderException e) {
                    throw new RuntimeException(e);
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                if(result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    Logger.debug("my log",result.getContents());
                    DigitalSeal digitalSealToVerify = DigitalSeal.fromRawString(result.getContents());
                    Verifier verifier = new Verifier(digitalSealToVerify, cert);
                    Verifier.Result result1 = verifier.verify();
                    Logger.debug(result1.toString());
                    expiryDateValueTv.setText("Issue Date : " + digitalSealToVerify.getIssuingDate().toString());
                    datOfBirthValueTv.setText("Signature Date: " + digitalSealToVerify.getSigDate().toString());
                    scannedValueTv.setText("Signature Status : " + result1.toString());
                }

            }

    );
    public static KeyStore getKeystore(InputStream is, String keyStorePassword) {
        KeyStore keystore;

        try {
            keystore = KeyStore.getInstance("BKS", "BC");
//            FileInputStream fis = new FileInputStream(keyStoreFile);
            keystore.load(is, keyStorePassword.toCharArray());
            //keystore.load(is, keyStorePassword.toCharArray());
            is.close();
            return keystore;
        } catch (KeyStoreException | NoSuchProviderException | NoSuchAlgorithmException | CertificateException
                 | IOException e) {
            Logger.warn("Error while opening keystore '" + "': " + e.getMessage());
            return null;
        }
    }
    public static FileInputStream convertInputStreamToFileInputStream(InputStream inputStream) throws IOException {
        // Create a temporary file
        File tempFile = File.createTempFile("ec_certificate", ".tmp");
        tempFile.deleteOnExit(); // Ensure the file is deleted when the program exits

        // Write the contents of the InputStream to the temporary file
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        }

        // Return a FileInputStream for the temporary file
        return new FileInputStream(tempFile);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

}