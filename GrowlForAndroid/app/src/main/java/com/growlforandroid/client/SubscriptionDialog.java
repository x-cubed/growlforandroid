package com.growlforandroid.client;

import com.growlforandroid.common.Subscription;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * The Add/Edit Subscription dialog
 * 
 * @author Carey
 * 
 */
public class SubscriptionDialog {
	private final Context _context;
	private final Listener _listener;

	private LayoutInflater _factory;
	private View _textEntryView;
	private EditText _txtName;
	private EditText _txtAddress;
	private EditText _txtPassword;

	private Builder _builder;

	private long _id;

	private SubscriptionDialog(Context context, Listener listener) {
		_context = context;
		_listener = listener;

		_factory = LayoutInflater.from(_context);
		_textEntryView = _factory.inflate(R.layout.add_subscription_dialog, null);
		_txtName = (EditText) _textEntryView.findViewById(R.id.txtSubscriptionName);
		_txtAddress = (EditText) _textEntryView.findViewById(R.id.txtSubscriptionAddress);
		_txtPassword = (EditText) _textEntryView.findViewById(R.id.txtSubscriptionPassword);

		_builder = new AlertDialog.Builder(_context).setView(_textEntryView).setNegativeButton(R.string.dialog_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						/* User clicked cancel so don't do anything */
					}
				});
	}

	public Dialog create() {
		return _builder.create();
	}

	public void updateFields(String name, String address, boolean editable, String password) {
		_txtName.setText(name);
		_txtName.setEnabled(editable);
		_txtName.setFocusable(editable);

		_txtAddress.setText(address);
		_txtAddress.setEnabled(editable);
		_txtAddress.setFocusable(editable);

		_txtPassword.setText(password);
	}

	public void populateAddDialog(final boolean isZeroConf) {
		_builder.setTitle(R.string.subscriptions_add_dialog_title);
		_builder.setPositiveButton(R.string.dialog_add, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Get the field values from the dialog
				String name = _txtName.getText().toString();
				String address = _txtAddress.getText().toString();
				String password = _txtPassword.getText().toString();
				if (name.equals("") || address.equals("") || password.equals("")) {
					return;
				}
				_listener.addSubscription(name, address, password, isZeroConf);
			}
		});
	}

	public static Dialog createAddDialog(Context context, Listener listener) {
		SubscriptionDialog builder = new SubscriptionDialog(context, listener);
		builder.updateFields("", "", true, "");
		builder.populateAddDialog(false);
		return builder.create();
	}

	public void populateEditDialog(long id) {
		_builder.setTitle(R.string.subscriptions_edit_dialog_title);
		_builder.setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Get the field values from the dialog
				String name = _txtName.getText().toString();
				String address = _txtAddress.getText().toString();
				String password = _txtPassword.getText().toString();
				if (name.equals("") || address.equals("") || password.equals("")) {
					return;
				}
				_listener.updateSubscription(_id, name, address, password);
			}
		});
	}

	public static Dialog createEditDialog(Context context, Listener listener, Subscription subscription) {
		SubscriptionDialog builder = new SubscriptionDialog(context, listener);
		builder.updateFields(subscription.getName(), subscription.getAddress(), !subscription.isZeroConf(),
				subscription.getPassword());
		boolean newZeroConf = subscription.isZeroConf() && !subscription.isSubscribed();
		if (newZeroConf) {
			builder.populateAddDialog(subscription.isZeroConf());
		} else {
			builder.populateEditDialog(subscription.getId());
		}
		return builder.create();
	}

	public interface Listener {
		public void addSubscription(String name, String address, String password, boolean isZeroConf);
		public void updateSubscription(long id, String name, String address, String password);
	}
}
