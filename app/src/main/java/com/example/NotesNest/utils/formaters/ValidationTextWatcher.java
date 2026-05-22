package com.example.NotesNest.utils.formaters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import com.example.NotesNest.utils.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Reusable TextWatcher for real-time validation.
 * It updates the TextInputLayout error message while typing.
 */
public class ValidationTextWatcher implements TextWatcher {

    private final TextInputLayout inputLayout;
    private final TextInputEditText editText;
    private final TextView errorTextView;
    private final FieldType fieldType;
    private final TextInputEditText passwordField; // only used for confirm password

    public ValidationTextWatcher(TextInputLayout layout, TextInputEditText editText, TextView errorTextView, FieldType type) {
        this(layout, editText, type, errorTextView, null);
    }

    public ValidationTextWatcher(TextInputLayout layout, TextInputEditText editText,
                                 FieldType type, TextView errorTextView, TextInputEditText passwordField) {
        this.inputLayout = layout;
        this.editText = editText;
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

        String errorMsg = null;

        switch (fieldType) {
            case EMAIL:
                if (!ValidationUtils.isValidEmail(text))
                    errorMsg = "Enter a valid email address";
                break;

            case USERNAME:
                if (!ValidationUtils.isValidUsername(text))
                    errorMsg = "3–15 chars, letters/numbers/underscore only";
                break;

            case PASSWORD:
                if (!ValidationUtils.isValidPassword(text))
                    errorMsg = "8+ chars, upper & lower case, number, symbol";
                break;

            case CONFIRM_PASSWORD:
                if (passwordField != null && !text.equals(passwordField.getText().toString().trim()))
                    errorMsg = "Passwords do not match";
                break;
        }

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
