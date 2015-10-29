package com.growlforandroid.client;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class PreferenceCheckBoxView
	extends PreferenceView {
	private CheckBox _chkCheckBox;
	
	public PreferenceCheckBoxView(Context context) {
		super(context);
	}
	
	public PreferenceCheckBoxView(Context context, AttributeSet attributes) {
		super(context, attributes);
	}
	
	@Override
	protected void initialise(Context context, AttributeSet attributes) {
		super.initialise(context, attributes);
		
		// Put a check box on the right
		_chkCheckBox = new CheckBox(context, null);
		LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		checkBoxParams.gravity = Gravity.CENTER_VERTICAL;
		addView(_chkCheckBox, checkBoxParams);
		
		// Clicking on the control toggles the check box
		setClickable(true);
		setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				_chkCheckBox.toggle();
			}
		});
	}

	@Override
	protected void applyAttributes(AttributeSet attributes) {
		super.applyAttributes(attributes);
		
		TypedArray a = getContext().obtainStyledAttributes(attributes, R.styleable.PreferenceCheckBoxView, 0, 0);
		setChecked(a.getBoolean(R.styleable.PreferenceCheckBoxView_checked, false));
		a.recycle();
	}
	
	public void setChecked(boolean checked) {
		_chkCheckBox.setChecked(checked);
	}
	
	public boolean isChecked() {
		return _chkCheckBox.isChecked();
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		_chkCheckBox.setOnCheckedChangeListener(listener);
	}
}
