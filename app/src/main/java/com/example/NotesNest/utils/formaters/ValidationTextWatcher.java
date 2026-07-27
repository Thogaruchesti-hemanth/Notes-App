package com.example.NotesNest.utils.formaters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.example.NotesNest.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

/**
 * Reusable TextWatcher for real-time validation.
 * It updates the TextInputLayout error message while typing.
 */
public class ValidationTextWatcher implements TextWatcher {

    private final TextInputLayout inputLayout;
    private final TextView errorTextView;
    private final FieldType fieldType;
    private final TextInputEditText passwordField; // only used for confirm password

    public ValidationTextWatcher(TextInputLayout layout, FieldType type, TextView errorTextView) {
        this(layout, type, errorTextView, null);
    }

    public ValidationTextWatcher(TextInputLayout layout, FieldType type,
                                 TextView errorTextView, TextInputEditText passwordField) {
        this.inputLayout = layout;
        this.fieldType = type;
        this.errorTextView = errorTextView;
        this.passwordField = passwordField;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (errorTextView != null) {
            errorTextView.setText("");
            errorTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
        String text = editable.toString().trim();

        // Check if empty (optional: might want to clear error if empty without showing new error)
        if (text.isEmpty()) {
            inputLayout.setError(null);
            inputLayout.setErrorEnabled(false);
            return;
        }

        String errorMsg = switch (fieldType) {
            case EMAIL -> ValidationUtils.isValidEmail(text) ? "Enter a valid email address" : null;
            case USERNAME -> ValidationUtils.isValidUsername(text) ? "3–15 chars, letters/numbers/underscore only" : null;
            case PASSWORD -> ValidationUtils.isValidPassword(text) ? "8+ chars, upper & lower case, number, symbol" : null;
            case CONFIRM_PASSWORD -> (passwordField != null && !text.equals(Objects.requireNonNull(passwordField.getText()).toString().trim()))
                    ? "Passwords do not match" : null;
        };

        if (errorMsg != null) {
            inputLayout.setError(errorMsg);
            inputLayout.setErrorEnabled(true);
        } else {
            inputLayout.setError(null);
            inputLayout.setErrorEnabled(false);
        }
    }

    public enum FieldType {EMAIL, USERNAME, PASSWORD, CONFIRM_PASSWORD}
}
