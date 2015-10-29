package com.growlforandroid.client;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.*;

abstract class PreferenceView
	extends LinearLayout {
	
	private boolean _loaded;
	protected TextView _txtTitle;
	protected TextView _txtSummary;
	
	public PreferenceView(Context context) {
		super(context);
		initialise(context, null);
	}
	
	public PreferenceView(Context context, AttributeSet attributes) {
		super(context, attributes);
		initialise(context, attributes);
		
		if (attributes != null) {
			applyAttributes(attributes);
		}
	}

	protected void initialise(Context context, AttributeSet attributes) {
		if (_loaded)
			return;
		_loaded = true;
		
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
	}
	
	protected void applyAttributes(AttributeSet attributes) {
		TypedArray a = getContext().obtainStyledAttributes(attributes, R.styleable.PreferenceView, 0, 0);
		setTitle(a.getString(R.styleable.PreferenceView_title));
		setSummary(a.getString(R.styleable.PreferenceView_summary));
		a.recycle();
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
}
