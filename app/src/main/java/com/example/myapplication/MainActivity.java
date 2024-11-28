package com.example.myapplication;

import android.os.Build;
import android.os.Bundle;

import com.example.myapplication.vdstools.DataEncoder;
import com.example.myapplication.vdstools.Verifier;
import com.example.myapplication.vdstools.vds.DigitalSeal;
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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private Button scanQrBtn;
    private TextView scannedValueTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        scanQrBtn = findViewById(R.id.scanQrBtn);
        scannedValueTv = findViewById(R.id.scannedValueTv);

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
                X509Certificate cert = null;
                String password_ = "bccca";
                FileInputStream fis = null;
                try {
                    InputStream rawInputStream = getResources().openRawResource(R.raw.ahad_cert);
                    fis = convertToInputStream(rawInputStream);

                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
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
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                // Initialize KeyStore

                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    Logger.debug("my log",result.getContents());
                    //scannedValueTv.setText("Scanned Value : " + result.toString());

                    DigitalSeal digitalSealToVerify = DigitalSeal.fromRawString(result.getContents());
//                    String vdsType = digitalSealToVerify.getVdsType();
//
//// getFeature() returns an Optional<Feature> which can be used as follows
//                    String mrzToVerify = digitalSealToVerify.getFeature("MRZ").get().asString();
//                    String azrToVerify = digitalSealToVerify.getFeature("AZR").get().asString();
//                    if(digitalSealToVerify.getFeature("FACE_IMAGE").isPresent() ){
//                        byte[] imgBytes = digitalSealToVerify.getFeature("FACE_IMAGE").get().asByteArray();
//                    }
//                    String signerCertRef = digitalSealToVerify.getSignerCertRef();
                    Verifier verifier = new Verifier(digitalSealToVerify, cert);
                    Verifier.Result result1 = verifier.verify();
                    Logger.debug(result1.toString());
                    scannedValueTv.setText("Scanned Value : " + result1.toString());
                }
            }
    );
    public FileInputStream convertToInputStream(InputStream inputStream) throws IOException {
        // Create a temporary file
        File tempFile = File.createTempFile("certificate", ".p12");
        tempFile.deleteOnExit(); // Ensure the file is deleted when the JVM exits

        // Write InputStream to the temporary file
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