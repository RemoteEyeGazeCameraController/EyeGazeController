package com.eyegaze.mousecontroller;

import javax.print.DocFlavor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.Timer;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import org.python.core.Py;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.python.modules.time.*;



public class MainApp {
    private JFrame frame;
    private JPanel panel;
    private JLabel connectedDeviceName, cameraState;
    private JButton onSwitchButton, offSwitchButton;

    private static Firestore db;
    private Map<String, Object> deviceState = new HashMap<>();
    private Boolean prevState;
    private Timer timer = new Timer();
    private int FirebaseInitCounter = 0;

    public MainApp() {
        initSDK();
        frame = new JFrame();
        panel = new JPanel();

        connectedDeviceName = new JLabel("device name");
        cameraState = new JLabel("STATE");
        onSwitchButton = new JButton("ON");
        offSwitchButton = new JButton("OFF");

        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
        panel.setLayout(new GridBagLayout());
        panel.setBackground(Color.white);

        connectedDeviceName.setFont(new Font("Roboto", Font.BOLD, 16));
        cameraState.setFont(new Font("Roboto", Font.BOLD, 16));
        onSwitchButton.setFont(new Font("Roboto", Font.BOLD, 28));
        offSwitchButton.setFont(new Font("Roboto", Font.BOLD, 28));

        onSwitchButton.setBackground(new Color(0x2dce98));
        onSwitchButton.setForeground(Color.white);
        offSwitchButton.setBackground(new Color(0xC01515));
        offSwitchButton.setForeground(Color.white);

        onSwitchButton.setUI(new StyledButtonUI());
        offSwitchButton.setUI(new StyledButtonUI());

    }

    /**
     * Create frame and all elements to the panel*/
    public void createFrame(){

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 0.5;
        constraints.insets = new Insets(5, 0, 5, 0);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;

        panel.add(onSwitchButton, constraints);
        panel.add(offSwitchButton, constraints);


        constraints.insets = new Insets(0, 4, 0, 5);
        constraints.gridwidth = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.gridx = GridBagConstraints.WEST;
        constraints.gridy = 100;

        panel.add(cameraState, constraints);
        constraints.gridx = GridBagConstraints.EAST;

        panel.add(connectedDeviceName, constraints);

        // Get screen size
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        // set dimension min size
        Dimension dim = new Dimension(600, 600);

        // get frame location
        int x = (screenSize.width - frame.getWidth())/3;
        int y = (screenSize.width - frame.getHeight())/6;

        // Add panel and set title
        frame.add(panel, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("EyeGaze Controller");

        // Set Icon
        ImageIcon img = new ImageIcon("src/main/resources/com/eyegaze/mousecontroller/eye.jpg");
        frame.setIconImage(img.getImage());

        // set location and min size of application
        frame.setLocation(x, y);
        frame.setMinimumSize(dim);

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        MainApp instance = new MainApp();
        instance.createFrame();

        DocumentReference docRef = db.collection("Camera Status").document("rh4foKVGNKpCFzMnb7Ss");
        PySystemState systemState = Py.getSystemState();
        //Link some external python libraries installed via maven plugin
        systemState.path.append(new PyString("target/classes/Lib"));

        // Add device name to hashmap and display name
        instance.deviceState.put("Connected Tobii Device", InetAddress.getLocalHost().getHostName());
        instance.connectedDeviceName.setText("Connected Device:    " + InetAddress.getLocalHost().getHostName());

        // Get current state of camera from Database and display it
        List<QueryDocumentSnapshot> documents = instance.processDB(docRef);
        for (QueryDocumentSnapshot document : documents) {
            if (document.getBoolean("Enabled")){
                instance.cameraState.setText("Camera State:    ON");
                instance.onSwitchButton.setEnabled(false);
                instance.offSwitchButton.setEnabled(true);
            } else {
                instance.cameraState.setText("Camera State:    OFF");
                instance.onSwitchButton.setEnabled(true);
                instance.offSwitchButton.setEnabled(false);
            }

            //instance.connectedDeviceName.setText("Connected Device: " + document.getString("Connected Tobii Device"));
            instance.prevState = document.getBoolean("Enabled");
            instance.deviceState.put("Enabled", document.getBoolean("Enabled"));
        }

        // when ON is clicked turn on the camera iff the camera state is OFF
        instance.onSwitchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<QueryDocumentSnapshot> documents = null;
                try {
                    documents = instance.processDB(docRef);
                    if(!documents.isEmpty()) {
                        for (QueryDocumentSnapshot document : documents) {
                            if (!document.getBoolean("Enabled")) {
                                instance.cameraState.setText("Camera State:    ON");
                                instance.deviceState.replace("Enabled", true);
                                instance.prevState = (Boolean) instance.deviceState.get("Enabled");
                                ApiFuture<WriteResult> result = docRef.update(instance.deviceState);
                                instance.onSwitchButton.setEnabled(false);
                                instance.offSwitchButton.setEnabled(true);
                                instance.turnOn();
                            }
                        }
                    } else{
                        JOptionPane.showMessageDialog(null, "There is an error with the database.\n Database elements might be empty please repopulate with the correct data.");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        // when OFF is clicked turn off the camera iff the camera state is ON
        instance.offSwitchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<QueryDocumentSnapshot> documents = null;
                try {
                    documents = instance.processDB(docRef);
                    if(!documents.isEmpty()) {
                        for (QueryDocumentSnapshot document : documents) {
                            if (document.getBoolean("Enabled")) {
                                instance.cameraState.setText("Camera State:    OFF");
                                instance.deviceState.replace("Enabled", false);
                                instance.prevState = (Boolean) instance.deviceState.get("Enabled");
                                ApiFuture<WriteResult> result = docRef.update(instance.deviceState);
                                instance.offSwitchButton.setEnabled(false);
                                instance.onSwitchButton.setEnabled(true);
                                instance.turnOff();
                            }
                        }
                    } else{
                            JOptionPane.showMessageDialog(null, "There is an error with the database.\n Database elements might be empty please repopulate with the correct data.");
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                }
            }
        });

        instance.frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    List<QueryDocumentSnapshot> documents = instance.processDB(docRef);
                    for (QueryDocumentSnapshot document : documents) {
                        instance.deviceState.replace("Connected Tobii Device", "");
                        ApiFuture<WriteResult> result = docRef.update(instance.deviceState);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        //Check the database for any changes every 1.5s and update state
        instance.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<QueryDocumentSnapshot> documents = instance.processDB(docRef);
                    for (QueryDocumentSnapshot document : documents) {
                        if (document.getBoolean("Enabled") != instance.prevState) {
                            instance.deviceState.replace("Enabled", document.getBoolean("Enabled"));
                            instance.prevState = (Boolean) instance.deviceState.get("Enabled");
                            ApiFuture<WriteResult> result = docRef.update(instance.deviceState);
                            if (document.getBoolean("Enabled")) {
                                instance.cameraState.setText("Camera State:    ON");
                                instance.onSwitchButton.setEnabled(false);
                                instance.offSwitchButton.setEnabled(true);
                                instance.turnOn();
                            } else{
                                instance.cameraState.setText("Camera State:    OFF");
                                instance.offSwitchButton.setEnabled(false);
                                instance.onSwitchButton.setEnabled(true);
                                instance.turnOff();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 10000, 15000);
    }

    /**
     * @Param docRef
     * @Return List
     * Process and query the database and return it in a list
     **/
    private List<QueryDocumentSnapshot> processDB(DocumentReference docRef) throws IOException {
        ApiFuture<QuerySnapshot> query = db.collection("Camera Status").get();
        QuerySnapshot querySnapshot = null;
        try {
            querySnapshot = query.get();
        } catch (InterruptedException | ExecutionException ex) {
            JOptionPane.showMessageDialog(null, "Can not fetch the data please check your internet connection or the database");
            ex.printStackTrace();
        }
        return querySnapshot.getDocuments();
    }

    /**
     * Intialize database and authorize credentials*/
    private void initSDK() {

        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream serviceAccount = classloader.getResourceAsStream("com/eyegaze/mousecontroller/serviceAccount.json");

            //InputStream serviceAccount = MainApp.class.getClassLoader().getResourceAsStream("/com/eyegaze/mousecontroller/serviceAccount.json");
            //FileInputStream serviceAccount = new FileInputStream("src/main/resources/serviceAccount.json");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://eyegazecameradb-default-rtdb.firebaseio.com")
                    .build();

            FirebaseApp fireApp = FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
            FirebaseInitCounter = 0;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Unable to connect to the firebase database, error with intializing the database");

            if( FirebaseInitCounter < 3){
                FirebaseInitCounter++;
                initSDK();
            } else {
                JOptionPane.showMessageDialog(null, "Tried to connect 4 consecutive times but failed");
                JOptionPane.showMessageDialog(null, e.getMessage());
                e.printStackTrace();
            }

        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = inputStream.read(buffer)) != -1; ) {
            result.write(buffer, 0, length);
        }
        // StandardCharsets.UTF_8.name() > JDK 7
        return result.toString(StandardCharsets.UTF_8);
    }

    /**
     * Call python Script to turn on the camera
     */
    private void turnOn() {
        try {
             ClassLoader classloader = Thread.currentThread().getContextClassLoader();
             InputStream stream = classloader.getResourceAsStream("com/eyegaze/mousecontroller/turnOn.py");
             String source = readStream(stream);
            //URL stream = getClass().getResource("/com/eyegaze/mousecontroller/turnOn.py");
            //String path = stream.toString();
            //String path2 = stream.getPath();
             JOptionPane.showMessageDialog(null, source);
            //JOptionPane.showMessageDialog(null, path);

             PythonInterpreter interp = new PythonInterpreter();
             interp.compile(source);

            //Process process = Runtime.getRuntime().exec("Python " + path);
//            String Script_Path = "src/main/resources/com/eyegaze/mousecontroller/turnOn.py";
//            ProcessBuilder Process_Builder = new
//                    ProcessBuilder("python",Script_Path)
//                    .inheritIO();

            //BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//            if (stdError.readLine() != null){
//                JOptionPane.showMessageDialog(null, "Error Detected: " + stdError.lines().collect(Collectors.joining()));
//            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,ex.getMessage());
            ex.printStackTrace();

        }
    }

    /**
     * Call python Script to turn off the camera
     */
    private void turnOff(){
        try {
            Process process = Runtime.getRuntime().exec("Python src/main/turnOff.py");
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            if (stdError.readLine() != null){
                JOptionPane.showMessageDialog(null, "Error Detected: " + stdError.lines().collect(Collectors.joining()));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
