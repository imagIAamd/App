package com.example.imageia;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import android.Manifest;
import android.widget.Toast;


public class InicioFragment extends Fragment {

    EditText editTextCorreo, editTextUsuario, editTextTelefono;
    Button btnContinuar;

    HttpPostManager.OnResponseListener listener;

    BottomNavigationView bottomNavigationView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.inicio, container, false);

        editTextCorreo = rootView.findViewById(R.id.editTextCorreo);
        editTextUsuario = rootView.findViewById(R.id.editTextUsuario);
        editTextTelefono = rootView.findViewById(R.id.editTextTelefono);
        btnContinuar = rootView.findViewById(R.id.btnContinuar);

        btnContinuar.setEnabled(false);

        editTextCorreo.addTextChangedListener(textWatcher);
        editTextUsuario.addTextChangedListener(textWatcher);
        editTextTelefono.addTextChangedListener(textWatcher);

        btnContinuar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String correo = editTextCorreo.getText().toString();
                String usuario = editTextUsuario.getText().toString();
                String telefono = editTextTelefono.getText().toString();

                // Guardar los datos en un archivo SVG
                saveDataToSVG(correo, usuario, telefono);

                // Enviar los datos del usuario
                sendUserDataToServer(correo, usuario, telefono);

            }
        });

        bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setVisibility(View.GONE);

        // Inicializar el listener para manejar las respuestas del servidor
        listener = new HttpPostManager.OnResponseListener() {
            @Override
            public void onResponse(String response) {
                // Manejar la respuesta del servidor aquí
            }

            @Override
            public void onError(Exception e) {
                // Manejar errores aquí
                Log.e("InicioFragment", "Error en la solicitud HTTP: " + e.getMessage());
            }
        };

        return rootView;
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            boolean camposCompletos = !TextUtils.isEmpty(editTextCorreo.getText())
                    && !TextUtils.isEmpty(editTextUsuario.getText())
                    && !TextUtils.isEmpty(editTextTelefono.getText());

            btnContinuar.setEnabled(camposCompletos);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bottomNavigationView.setVisibility(View.VISIBLE);
    }
    /*
        private void verificar(String correo, String usuario, String telefono) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Ingrese el código");

            final EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    goToProfileFragment(correo, usuario, telefono);
                }

            });

            builder.show();
        }
    */
    private void saveDataToSVG(String correo, String usuario, String telefono) {
        // Creamos el contenido SVG con los datos
        String svgContent = "<svg width=\"100\" height=\"100\">\n" +
                "  <text x=\"10\" y=\"20\">Correo: " + correo + "\n" +
                "  <text x=\"10\" y=\"40\">Usuario: " + usuario + "\n" +
                "  <text x=\"10\" y=\"60\">Teléfono: " + telefono + "\n" +
                "</svg>";

        // Guardamos el contenido SVG en un archivo
        File file = new File(requireContext().getFilesDir(), "data.svg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(svgContent);
            writer.close();
            fos.close();
            Log.d("InicioFragment", "Datos guardados en archivo SVG");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendUserDataToServer(String correo, String usuario, String telefono) {
        try {
            // Crear el objeto JSON con los datos del usuario
            JSONObject userData = new JSONObject();
            userData.put("nickname", usuario);
            userData.put("email", correo);
            userData.put("phone_number", telefono);

            // Enviar la solicitud al servidor
            HttpPostManager.sendPostRequest("https://ams26.ieti.site/api/maria/user/register", userData, new HttpPostManager.OnResponseListener() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.getString("status");
                        if (status.equals("OK")) {
                            // Si el estado es "OK", mostrar el diálogo y enviar la solicitud de validación
                            mostrarDialogoValidacion(correo, usuario, telefono);
                        }
                    } catch (JSONException e) {
                        Log.e("HttpPostRequest", "Error: " + e.getMessage());
                        showConnectionErrorToast();

                    }
                }
                @Override
                public void onError(Exception e) {
                    Log.e("HttpPostRequest", "Error: " + e.getMessage());
                    // Manejar errores de la solicitud HTTP
                    showConnectionErrorToast();
                }
            });

            System.out.println(userData.toString(5));
        } catch (JSONException e) {
            Log.e("HttpPostRequest", "Error: " + e.getMessage());
            showConnectionErrorToast();
        }
    }


    private void goToProfileFragment(String correo, String usuario, String telefono) {
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, ProfileFragment.newInstance());
        transaction.commit();
    }

    private void mostrarDialogoValidacion(String correo, String usuario, String telefono) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ingrese el código de validación");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String codigoValidacionStr = input.getText().toString();
                try {
                    int codigoValidacion = Integer.parseInt(codigoValidacionStr);
                    // Aquí puedes enviar la solicitud de validación al servidor
                    sendValidationRequest(correo, usuario, telefono, codigoValidacion);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    // El texto ingresado no es un número válido
                    Toast.makeText(requireContext(), "Por favor, ingrese un código de validación válido", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }


    private void sendValidationRequest(String correo, String usuario, String telefono, int codigoValidacion) {
        try {
            // Crear el objeto JSON con los datos del usuario
            JSONObject userValidation = new JSONObject();
            userValidation.put("phone_number", telefono);
            userValidation.put("validation_code", codigoValidacion);

            System.out.println(userValidation);

            // Enviar la solicitud de validación al servidor
            HttpPostManager.sendPostRequest("https://ams26.ieti.site/api/maria/user/validate", userValidation, new HttpPostManager.OnResponseListener() {
                @Override
                public void onResponse(String response) {
                    // Manejar la respuesta del servidor a la solicitud de validación
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.getString("message");
                        if (status.equals("User successfully validated")) {
                            // Si el estado es "OK", mostrar el diálogo y enviar la solicitud de validación
                            JSONObject data = jsonResponse.getJSONObject("data");
                            String apyKey = data.getString("access_key");
                            saveApyKey(apyKey);
                            goToProfileFragment(correo, usuario, telefono);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                    // Manejar errores de la solicitud HTTP
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void leerSMS() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_SMS}, 123);
        } else {
            Uri uri = Uri.parse("content://sms/inbox");
            Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, "date DESC");
            if (cursor != null && cursor.moveToFirst()) {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                try {
                    // Suponiendo que el SMS contiene solo una serie de números
                    String serieNumeros = body.trim();
                    // Procesar la serie de números como desees
                    // Por ejemplo, mostrarla en un diálogo
                } finally {
                    cursor.close();
                }
            }
        }
    }

    // Método para mostrar un Toast
    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Método para mostrar un Toast de error de conexión en el hilo principal
    private void showConnectionErrorToast() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                showToast("No se pudo conectar al servidor");
            }
        });
    }

    private void saveApyKey(String key) {
        // Creamos el contenido SVG con los datos
        String svgContent = "<svg width=\"100\" height=\"100\">\n" +
                "  <text x=\"10\" y=\"20\">Key: " + key +
                "</svg>";

        // Guardamos el contenido SVG en un archivo
        File file = new File(requireContext().getFilesDir(), "key.svg");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(svgContent);
            writer.close();
            fos.close();
            Log.d("InicioFragment", "Datos guardados en archivo SVG");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
