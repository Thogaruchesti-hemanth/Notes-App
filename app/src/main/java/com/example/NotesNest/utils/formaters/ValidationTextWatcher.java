package com.example.NotesNest.utils.formaters;

import android.text.Editable;
import android.text.TextWatcher;
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
        errorTextView.setText("");
    }

    @Override
    public void afterTextChanged(Editable editable) {
        String text = editable.toString().trim();
        inputLayout.setError(null); // clear old error

        switch (fieldType) {
            case EMAIL:
                if (!ValidationUtils.isValidEmail(text))
                    inputLayout.setError("Enter a valid email address");
                break;

            case USERNAME:
                if (!ValidationUtils.isValidUsername(text))
                    inputLayout.setError("3–15 chars, letters/numbers/underscore only");
                break;

            case PASSWORD:
                if (!ValidationUtils.isValidPassword(text))
                    inputLayout.setError("8+ chars, upper & lower case, number, symbol");
                break;

            case CONFIRM_PASSWORD:
                if (passwordField != null && !text.equals(passwordField.getText().toString().trim()))
                    inputLayout.setError("Passwords do not match");
                break;
        }
    }

    public enum FieldType {EMAIL, USERNAME, PASSWORD, CONFIRM_PASSWORD}
}
