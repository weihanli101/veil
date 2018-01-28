package io.left.hellomesh;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import io.left.rightmesh.android.AndroidMeshManager;
import io.left.rightmesh.android.MeshService;
import io.left.rightmesh.id.MeshID;
import io.left.rightmesh.mesh.MeshManager;
import io.left.rightmesh.mesh.MeshStateListener;
import io.left.rightmesh.util.RightMeshException;
import io.reactivex.functions.Consumer;

import static io.left.rightmesh.mesh.MeshManager.DATA_RECEIVED;
import static io.left.rightmesh.mesh.MeshManager.PEER_CHANGED;
import static io.left.rightmesh.mesh.MeshManager.REMOVED;

public class PatientActivity extends Activity implements MeshStateListener{
    private class UserInfo {
        String name;
        String role;

        public UserInfo (String _name, String _role){
            name = _name;
            role = _role;
        }
    }

    private static final int PORT = 9876;

    // MeshManager instance - interface to the mesh network.
    AndroidMeshManager mm = null;
    // Set to keep track of peers connected to the mesh.
    HashSet<MeshID> users = new HashSet<>();
    HashMap<MeshID, UserInfo> userInfo = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);

        mm = AndroidMeshManager.getInstance(PatientActivity.this, PatientActivity.this);
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
            //shuts down Android Mesh Manager
            mm.stop();
        } catch (MeshService.ServiceDisconnectedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void meshStateChanged(MeshID uuid, int state) {
        if (state == MeshStateListener.SUCCESS) {
            try {
                // Binds this app to MESH_PORT.
                // This app will now receive all events generated on that port.
                mm.bind(PORT);

                // Subscribes handlers to receive events from the mesh.
                mm.on(DATA_RECEIVED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handleDataReceived((MeshManager.RightMeshEvent) o);
                    }
                });

                mm.on(PEER_CHANGED, new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {
                        handlePeerChanged((MeshManager.RightMeshEvent) o);
                    }
                });

            } catch (RightMeshException e) {
                return;
            }
        }
    }

    private void handleDataReceived(MeshManager.RightMeshEvent e) {
        final MeshManager.DataReceivedEvent event = (MeshManager.DataReceivedEvent) e;
        String peerUuid = String.valueOf(event.peerUuid);
        String data = new String(event.data);
        String[] args = data.split(";");
        System.out.println("event from Paramedic: " + event.data);

       /* if (args[0].equals("receiveInfo")) {
            String name = args[1];
            String role = args[2];

            if (role.equals("paramedic")) {
                userInfo.put(event.peerUuid, new PatientActivity.UserInfo(name, role));

                TextView patientList = (TextView) findViewById(R.id.user_profile_name);
                patientList.append("Peer Id: " + peerUuid + "\n" + data + "\n\n");
            }
        } else */if (args[0].equals("getInfo")) {
            try {
                mm.sendDataReliable(event.peerUuid, PORT, ("receiveInfo;sam" + ";patient;").getBytes());
            } catch (RightMeshException re) {}
        }
    }


    private void handlePeerChanged(MeshManager.RightMeshEvent e) {
        // Update peer list.
        MeshManager.PeerChangedEvent event = (MeshManager.PeerChangedEvent) e;
        System.out.println("peer: " + String.valueOf(event.peerUuid));
        if (event.state != REMOVED && !users.contains(event.peerUuid)) {
            users.add(event.peerUuid);

            try {
                mm.sendDataReliable(event.peerUuid, PORT, "getInfo".getBytes());
            } catch(RightMeshException re) {}

        } else if (event.state == REMOVED){
            users.remove(event.peerUuid);
            userInfo.remove(event.peerUuid);
        }


        String output = "";
        for (MeshID user : users) {
            output += user;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_patient, menu);
        return true;
    }


}
