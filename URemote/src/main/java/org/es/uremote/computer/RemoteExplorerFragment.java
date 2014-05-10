package org.es.uremote.computer;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import org.es.uremote.Computer;
import org.es.uremote.R;
import org.es.uremote.common.AbstractExplorerFragment;
import org.es.uremote.device.ServerSetting;
import org.es.uremote.exchange.Message.Request;
import org.es.uremote.exchange.Message.Request.Code;
import org.es.uremote.exchange.Message.Request.Type;
import org.es.uremote.exchange.Message.Response;
import org.es.uremote.exchange.MessageUtils;
import org.es.uremote.exchange.RequestSender;
import org.es.uremote.network.AsyncMessageMgr;
import org.es.uremote.utils.TaskCallbacks;
import org.es.utils.Log;

import static org.es.uremote.exchange.Message.Response.ReturnCode.RC_ERROR;

/**
 * Remote file explorer fragment.
 * This fragment allow you to browse the content of a remote device.
 *
 * @author Cyril Leroux
 * Created on 21/04/12.
 */
public class RemoteExplorerFragment extends AbstractExplorerFragment implements RequestSender {

	private static final String TAG = "RemoteExplorerFragment";

	private TaskCallbacks mCallbacks;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallbacks = (TaskCallbacks) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Retain this fragment across configuration changes.
		setRetainInstance(true);
	}

	/**
	 * Set the callback to null so we don't accidentally leak the
	 * Activity instance.
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}

	@Override
	protected void onDirectoryClick(String dirPath) {
		navigateTo(dirPath);
	}

	@Override
	protected void onFileClick(String filename) {

		Request request = null;
		try {
			MessageUtils.buildRequest(
                    getSecurityToken(),
                    Type.EXPLORER,
                    Code.OPEN_SERVER_SIDE,
                    Code.NONE,
                    filename);

		} catch (Exception e) {
			Toast.makeText(getActivity(), R.string.build_request_error, Toast.LENGTH_LONG).show();
			return;
		}
		sendRequest(request);
	}

	/**
	 * Ask the server to list the content of the passed directory.
	 * Updates the view once the data have been received from the server.
	 *
	 * @param dirPath The path of the directory to display.
	 */
	@Override
	protected void navigateTo(String dirPath) {
		if (dirPath != null) {
			sendRequest(MessageUtils.buildRequest(getSecurityToken(), Type.EXPLORER, Code.QUERY_CHILDREN, Code.NONE, dirPath));
		} else {
            Toast.makeText(getActivity(), R.string.msg_no_path_defined, Toast.LENGTH_SHORT).show();
        }
	}

	////////////////////////////////////////////////////////////////////
	// *********************** Message Sender *********************** //
	////////////////////////////////////////////////////////////////////

	/**
	 * Initializes the message handler then send the request.
	 *
	 * @param request The request to send.
	 */
	@Override
	public void sendRequest(Request request) {
		if (ExplorerMessageMgr.availablePermits() > 0) {
			new ExplorerMessageMgr().execute(request);
		} else {
			Toast.makeText(getActivity(), R.string.msg_no_more_permit, Toast.LENGTH_SHORT).show();
		}
	}

    @Override
    public ServerSetting getServerSetting() {
        return ((Computer)mCallbacks).getServer();
    }

    public String getSecurityToken() {
        ServerSetting settings = getServerSetting();
        if (settings != null) {
            return settings.getSecurityToken();
        }
        return null;
    }

	/**
	 * Class that handle asynchronous requests sent to a remote server.
	 *
	 * @author Cyril Leroux
	 */
	private class ExplorerMessageMgr extends AsyncMessageMgr {

		public ExplorerMessageMgr() {
            super(getServerSetting());
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mCallbacks.onPreExecute();
		}

		@Override
		protected void onPostExecute(Response response) {
			super.onPostExecute(response);
			mCallbacks.onPostExecute(response);

			Log.debug(TAG, "#onPostExecute - " + response.getMessage());
			if (RC_ERROR.equals(response.getReturnCode())) {
				if (!response.getMessage().isEmpty()) {
					Toast.makeText(getActivity(), response.getMessage(), Toast.LENGTH_LONG).show();
				}
				return;
			}

			if (!response.getMessage().isEmpty()) {
				Toast.makeText(getActivity(), response.getMessage(), Toast.LENGTH_LONG).show();
			}
			mCurrentFileInfo = response.getFile();
			updateView(response.getFile());

		}
	}
}
