package org.es.uremote.components;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.es.uremote.R;
import org.es.uremote.objects.ServerSetting;

import java.util.List;

/**
 * Adapter used to display server list.
 *
 * @author Cyril Leroux
 * Created on 22/05/13.
 */
public class ServerArrayAdapter extends ArrayAdapter<ServerSetting> {

	private final LayoutInflater mInflater;

	/**
	 * Default constructor
	 *
	 * @param context The application context.
	 * @param servers The list of {@link org.es.uremote.objects.ServerSetting} to display.
	 */
	public ServerArrayAdapter(Context context, List<ServerSetting> servers) {
		super(context, 0, servers);
		mInflater = LayoutInflater.from(context);
	}

	/** Template for the list items. */
	public static class ViewHolder {
		// TODO add thumbnail
		//ImageView ivThumbnail;
		TextView tvName;
		TextView tvLocalhost;
		TextView tvRemoteHost;
		TextView tvMacAddress;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.server_item, null);
			holder = new ViewHolder();
			// TODO add thumbnail
			// holder.ivThumbnail	= (ImageView) convertView.findViewById(R.id.ivThumbnail);
			holder.tvName		= (TextView) convertView.findViewById(R.id.server_name);
			holder.tvLocalhost	= (TextView) convertView.findViewById(R.id.local_host);
			holder.tvRemoteHost	= (TextView) convertView.findViewById(R.id.remote_host);
			holder.tvMacAddress	= (TextView) convertView.findViewById(R.id.mac_address);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final ServerSetting server = getItem(position);
		// TODO add thumbnail
		holder.tvName.setText(server.getName());
		holder.tvLocalhost.setText(server.getFullLocal());
		holder.tvRemoteHost.setText(server.getFullRemote());
		holder.tvMacAddress.setText(server.getMacAddress());
		return convertView;
	}
}