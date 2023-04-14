package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient gsc;
    private static final int RC_SIGN_IN = 60; //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // initialise firebase instance
        mAuth = FirebaseAuth.getInstance();
        // configure google sign in options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        // build a sign in client with previously configured options
        gsc = GoogleSignIn.getClient(this, gso);

        SignInButton signIn = findViewById(R.id.googleAuth);
        signIn.setOnClickListener(view -> signIn()); // on sign in button click, start google sign in
    }

    @Override
     public void onStart() {
        super.onStart();
        // check for and redirect signed in users
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(AuthActivity.this, SplashActivity.class));
        }
    }

    // google sign in
    private void signIn() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN); // deprecated method, however new approach seems to break and complicate things
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // returned result from launching GoogleSignInApi.getSignInIntent();
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // success, authenticate with firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("AUTH", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // failure
                Toast.makeText(AuthActivity.this, "Google sign in failed.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // firebase authentication
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnSuccessListener(this, authResult -> {
                    // success, launch the splash screen
                    startActivity(new Intent(AuthActivity.this, SplashActivity.class));
                    finish();
                }).addOnFailureListener(this, e ->
                    // failure
                    Toast.makeText(AuthActivity.this, "Firebase authentication failed.", Toast.LENGTH_LONG).show());
    }
}