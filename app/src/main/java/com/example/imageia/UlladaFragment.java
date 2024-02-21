package com.example.imageia;

import static android.app.ProgressDialog.show;

import static com.example.imageia.HttpPostManager.textToSpeech;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UlladaFragment extends Fragment implements SensorEventListener {

    // Executor para ejecutar tareas en un hilo diferente
    private Executor executor = Executors.newSingleThreadExecutor();
    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};

    private PreviewView mPreviewView; // Vista previa de la cámara
    private SensorManager sensorManager; // Gestor del sensor
    private Sensor accelerometer; // Sensor de aceleración
    ProcessCameraProvider cameraProvider;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    Preview preview;
    Camera camera;
    HttpPostManager.OnResponseListener listener;

    private boolean toastShown = false; // Bandera para controlar la visualización de los toasts
    private long lastTime = 0; // Último tiempo en el que se detectó un movimiento
    private float lastX, lastY, lastZ; // Últimos valores de aceleración
    private static final int SHAKE_THRESHOLD = 170; // Potencia del toque
    private static final int TIME_THRESHOLD = 150; // Tiempo entre toques
    private static final int SHAKE_TIME_THRESHOLD = 1000; // Tiempo para considerar un doble toque

    private ImageCapture imageCapture; // Variable de instancia para ImageCapture
    private Button captureImage; // Botón para capturar la imagen



    // Método para "inflar" el diseño de fragmento y devolver su vista
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ullada, container, false);
        mPreviewView = view.findViewById(R.id.preview);
        captureImage = view.findViewById(R.id.captureImg);
        preview = null; //Ini

        // Configuración del sensor de aceleración
        sensorManager = (SensorManager) requireContext().getSystemService(requireContext().SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        //Post Request
        listener = new HttpPostManager.OnResponseListener() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject r = new JSONObject(response);
                    String msn = r.getString("aggregatedResponse");
                    textToSpeech.speak(msn, TextToSpeech.QUEUE_ADD, null, null);
                } catch (Exception e) {
                    Log.e("HttpPostRequest", "Error: " + e.getMessage());
                }

                Log.d("HttpPostRequest", "Response: " + response);
            }

            @Override
            public void onError(Exception e) {
                // Handle error here, e.g., display error message to user
                Log.e("HttpPostRequest", "Error: " + e.getMessage());
            }
        };
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override      // Método llamado después de que se haya creado la vista del fragmento
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (allPermissionsGranted()) {
            initializeCamera(); // Iniciar la cámara si el usuario ha otorgado los permisos
            initiateTextSpeech();
        } else {
            ActivityCompat.requestPermissions(requireActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    // Método para inicializar la cámara
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void initializeCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));

    }

    private void initiateTextSpeech() {
        textToSpeech = new TextToSpeech(requireActivity().getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Engine is ready for use
                } else {}
            }
        });
        textToSpeech.setLanguage(Locale.US); // Set language
        textToSpeech.setPitch(1.0f); // Set pitch
        textToSpeech.setSpeechRate(0.8f); // Set speech rate
    }

    // Método para enlazar la vista previa con la cámara
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void bindPreview() {
        // Configurar la vista previa de la cámara
        preview = new Preview.Builder().build();

        // Selector de la cámara trasera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();

        // Constructor de captura de imagen
        ImageCapture.Builder builder = new ImageCapture.Builder();

        // Inicializar imageCapture
        imageCapture = builder
                .setTargetRotation(requireActivity().getWindowManager().getDefaultDisplay().getRotation())
                .build();

        // Captura final de la imagen para utilizar en el cierre
        final ImageCapture imageCaptureFinal = imageCapture; // Captura final para utilizar en el cierre

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        // Vincular la cámara al fragmento
        if (camera == null){
            camera = cameraProvider.bindToLifecycle(
                    (LifecycleOwner) requireActivity(), cameraSelector,
                    preview, imageAnalysis, imageCaptureFinal);
        }

        //Boton para capturar imagen
        captureImage.setOnClickListener(v -> {
            makeFoto();
        });
    }

    // Método para verificar si se dan todos los permisos necesarios
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Método para manejar el resultado de la solicitud de permisos
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeCamera();
            } else {
                Toast.makeText(requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                requireActivity().finish();
            }
        }
    }

    // Método para capturar una foto
    private void makeFoto() {
        if (imageCapture != null) {
            SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            File file = new File(requireActivity().getFilesDir(), mDateFormat.format(new Date()) + ".jpg");
            Log.d("Etiqueta", file.getPath());

            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

            try {
                imageCapture.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Notificar al usuario que la imagen se ha guardado exitosamente
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(requireContext(), "Image Saved successfully", Toast.LENGTH_SHORT).show());
                        sendPicture(file);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException error) {
                        error.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("UlladaFragment", "imageCapture is null");
        }
    }

    //Funcion para enviar imagen al server
    public void sendPicture(File image) {
        try {
            String imageBase64 = HttpPostManager.encodeImageToBase64(image);
            String prompt = "Describe this image";
            String token = "A000000000";

            // List of base64 encoded images
            List<String> imageList = new ArrayList<>();
            imageList.add(imageBase64);

            // Create the data object
            JSONObject data = new JSONObject()
                    .put("prompt", prompt)
                    .put("token", token)
                    .put("images", new JSONArray(imageList));

            HttpPostManager.sendPostRequest("https://ams22.ieti.site/data", data, listener);
        } catch (Exception e) {
            e.printStackTrace(); // Handle exception properly
        }
    }


    // Métodos de la interfaz SensorEventListener
    @Override
    public void onSensorChanged(SensorEvent event) {
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastTime;

        if (timeDifference > TIME_THRESHOLD) {
            lastTime = currentTime;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float deltaX = x - lastX;
            float deltaY = y - lastY;
            float deltaZ = z - lastZ;

            lastX = x;
            lastY = y;
            lastZ = z;

            double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) / timeDifference * 10000;

            if (speed > SHAKE_THRESHOLD) {
                long shakeTime = System.currentTimeMillis();
                if (shakeTime - lastTime < SHAKE_TIME_THRESHOLD && !toastShown) {
                    // Se ha detectado doble toque, capturar una foto
                    makeFoto();
                    toastShown = true;

                    // La notificación de la sacudida se podrá mostrar nuevamente después de 2 segundos
                    new Handler().postDelayed(() -> toastShown = false, 2000);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // Método llamado cuando se reanuda el fragmento
    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // Método llamado cuando se pausa el fragmento
    @Override
    public void onPause() {
        super.onPause();
        textToSpeech.shutdown();
        if (sensorManager != null) {
            Log.i("ULLADA","onPause sensor!=null");
            sensorManager.unregisterListener(this);
        }
        if (cameraProviderFuture != null) {
            Log.i("ULLADA","onPause cameraProviderFuture!=null");
            cameraProvider.unbindAll();
            cameraProviderFuture.cancel(true); // Cancel the future to release resources
            cameraProviderFuture = null; // Release reference to the future

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        textToSpeech.shutdown();
        Log.i("ULLADA", "onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        textToSpeech.shutdown();
        Log.i("ULLADA","onDestroy");
    }
}
