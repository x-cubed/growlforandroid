package com.growlforandroid.client;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class PreferenceCheckBoxView
	extends LinearLayout {

	private TextView _txtTitle;
	private TextView _txtSummary;
	private CheckBox _chkCheckBox;
	
	public PreferenceCheckBoxView(Context context) {
		super(context);
		populateView(context);
	}
	
	public PreferenceCheckBoxView(Context context, AttributeSet attributes) {
		super(context, attributes);
		populateView(context);
		
		if (attributes != null) {
			TypedArray a = getContext().obtainStyledAttributes(attributes, R.styleable.PreferenceCheckBoxView, 0, 0);
			setTitle(a.getString(R.styleable.PreferenceCheckBoxView_title));
			setSummary(a.getString(R.styleable.PreferenceCheckBoxView_summary));
			setChecked(a.getBoolean(R.styleable.PreferenceCheckBoxView_checked, false));
			a.recycle();
		}
	}
	
	private void populateView(Context context) {
		// The large text label is the title
		_txtTitle = new TextView(context, null, android.R.attr.textAppearanceLarge);
		_txtTitle.setEllipsize(TruncateAt.MARQUEE);
		_txtTitle.setHorizontalFadingEdgeEnabled(true);
		_txtTitle.setId(R.id.txtNotificationTitle);
		_txtTitle.setSingleLine(true);
		_txtTitle.setText("Title");
		
		// The smaller text label is the summary
		_txtSummary = new TextView(context, null, android.R.attr.textAppearanceSmall);
		_txtSummary.setEllipsize(TruncateAt.MARQUEE);
		_txtSummary.setHorizontalFadingEdgeEnabled(true);
		RelativeLayout.LayoutParams summaryParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		summaryParams.addRule(RelativeLayout.BELOW, _txtTitle.getId());
		summaryParams.addRule(RelativeLayout.ALIGN_LEFT, _txtTitle.getId());
		_txtSummary.setMaxLines(2);
		_txtSummary.setText("Summary");

		// Create a layout to put the text labels in
		RelativeLayout layout = new RelativeLayout(context);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(15, 6, 6, 6);
		layoutParams.weight = 1;
		
		// Add the labels to the layout, then add the layout to this
		layout.addView(_txtTitle, new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		layout.addView(_txtSummary, summaryParams);		
		addView(layout, layoutParams);		
		
		// Put a check box on the right
		_chkCheckBox = new CheckBox(context);
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

	public void setSummary(CharSequence title) {
		_txtSummary.setText(title);
	}
	
	public void setSummary(int resid) {
		_txtSummary.setText(resid);
	}
	
	public void setTitle(CharSequence title) {
		_txtTitle.setText(title);
	}
	
	public void setTitle(int resid) {
		_txtTitle.setText(resid);
	}
	
	public void setChecked(boolean checked) {
		_chkCheckBox.setChecked(checked);
	}
	
	public boolean isChecked() {
		return _chkCheckBox.isChecked();
	}
}
