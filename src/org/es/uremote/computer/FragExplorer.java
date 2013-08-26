package org.es.uremote.computer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.InvalidProtocolBufferException;

import org.es.uremote.exchange.ExchangeMessages.DirContent;
import org.es.uremote.exchange.ExchangeMessages.Request;
import org.es.uremote.exchange.ExchangeMessages.Request.Code;
import org.es.uremote.exchange.ExchangeMessages.Request.Type;
import org.es.uremote.exchange.ExchangeMessages.Response;
import org.es.uremote.exchange.RequestSender;
import org.es.uremote.Computer;
import org.es.uremote.R;
import org.es.uremote.components.FileManagerAdapter;
import org.es.uremote.dao.ServerSettingDao;
import org.es.uremote.network.AsyncMessageMgr;
import org.es.uremote.network.MessageHelper;
import org.es.utils.FileUtils;
import org.es.uremote.utils.TaskCallbacks;
import org.es.utils.Log;

import java.io.File;

import static org.es.uremote.exchange.ExchangeMessages.DirContent.File.FileType.DIRECTORY;
import static org.es.uremote.exchange.ExchangeMessages.Request.Code.NONE;
import static org.es.uremote.exchange.ExchangeMessages.Response.ReturnCode.RC_ERROR;

/**
 * File explorer fragment.
 * This fragment allow you to browse your PC content through the application.
 *
 * @author Cyril Leroux
 */
public class FragExplorer extends ListFragment implements RequestSender {

	private static final String TAG = "FragExplorer";

	private static final String DEFAULT_PATH		= "";
	private static final String DIRECTORY_CONTENT	= "DIRECTORY_CONTENT";

	private TaskCallbacks mCallbacks;

	private TextView mTvPath;
	private DirContent mDirectoryContent = null;

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
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.server_frag_explorer, container, false);
		mTvPath = (TextView) view.findViewById(R.id.tvPath);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Restoring current directory content
		if (savedInstanceState != null) {
			try {
				byte[] dirContent = savedInstanceState.getByteArray(DIRECTORY_CONTENT);
				if (dirContent != null) {
					mDirectoryContent = DirContent.parseFrom(savedInstanceState.getByteArray(DIRECTORY_CONTENT));
				}
			} catch (InvalidProtocolBufferException e) {
				Log.error(TAG, "#onActivityCreated - Error occurred while parsing directory content.", e);
			}
		}
		// Get the directory content from the server or update the one that already exist.
		if (mDirectoryContent == null) {
			openDirectory(DEFAULT_PATH);
		} else {
			updateView(mDirectoryContent);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (mDirectoryContent != null) {
			outState.putByteArray(DIRECTORY_CONTENT, mDirectoryContent.toByteArray());
		}
		super.onSaveInstanceState(outState);
	}

	/**
	 * Update the view with the content of the new directory
	 *
	 * @param dirContent The object that hosts the directory content.
	 */
	private void updateView(final DirContent dirContent) {
		if (dirContent == null) {
			return;
		}
		if (dirContent.getFileCount() == 0) {
			Log.warning(TAG, "#updateView - No file in the directory.");
			return;
		}

		final FileManagerAdapter adapter = new FileManagerAdapter(getActivity().getApplicationContext(), dirContent);
		setListAdapter(adapter);

		ListView listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final DirContent.File file = dirContent.getFile(position);
				final String filename = file.getName();
				final String currentDirPath = dirContent.getPath();

				if (DIRECTORY.equals(file.getType())) {

					if ("..".equals(filename)) {
						openParentDirectory(currentDirPath);
					} else {
						final String dirPath = currentDirPath + File.separator + filename;
						openDirectory(dirPath);
					}

				} else {
					// Open the file with the default program.
					final String fullPath = currentDirPath + File.separator + filename;
					openFile(fullPath);

				}
			}
		});

		mTvPath.setText(mDirectoryContent.getPath());
	}

	/**
	 * Ask the server to list the content of the passed directory.
	 * Updates the view once the data have been received from the server.
	 *
	 * @param dirPath The path of the directory to display.
	 */
	private void openDirectory(String dirPath) {
		sendAsyncRequest(MessageHelper.buildRequest(AsyncMessageMgr.getSecurityToken(), Type.EXPLORER, Code.GET_FILE_LIST, NONE, dirPath));
	}

	/**
	 * Ask the server to list the content of the passed directory's parent.
	 * Updates the view once the data have been received from the server.
	 *
	 * @param dirPath The path of the child directory.
	 */
	private void openParentDirectory(String dirPath) {
		final String parentPath = FileUtils.truncatePath(dirPath);
		sendAsyncRequest(MessageHelper.buildRequest(AsyncMessageMgr.getSecurityToken(), Type.EXPLORER, Code.GET_FILE_LIST, NONE, parentPath));
	}

	/**
	 * Ask the server to list the content of the current directory's parent.
	 * This method is supposed to be called from the {@link Computer} class.
	 * Updates the view once the data have been received from the server.
	 *
	 * @return true if it is possible to navigate up.
	 */
	public boolean navigateUpIfPossible() {
		if (mDirectoryContent == null) {
			return false;
		}
		final String dirPath = mDirectoryContent.getPath();
		if (dirPath.contains(File.separator)) {
			openParentDirectory(dirPath);
			return true;
		}
		return false;
	}

	private void openFile(String filename) {
		sendAsyncRequest(MessageHelper.buildRequest(AsyncMessageMgr.getSecurityToken(), Type.EXPLORER, Code.OPEN_FILE, NONE, filename));
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
	public void sendAsyncRequest(Request request) {
		if (ExplorerMessageMgr.availablePermits() > 0) {
			new ExplorerMessageMgr(Computer.getHandler()).execute(request);
		} else {
			Toast.makeText(getActivity().getApplicationContext(), R.string.msg_no_more_permit, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Class that handle asynchronous requests sent to a remote server.
	 *
	 * @author Cyril Leroux
	 */
	private class ExplorerMessageMgr extends AsyncMessageMgr {

		public ExplorerMessageMgr(Handler handler) {
			super(handler, ServerSettingDao.loadFromPreferences(getActivity().getApplicationContext()));
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
					sendToastToUI(response.getMessage());
				}
				return;
			}
			//TODO clean
			//} else if (_response.hasRequest() && GET_FILE_LIST.equals(response.getRequest().getCode())) {
			if (!response.getMessage().isEmpty()) {
				sendToastToUI(response.getMessage());
			}
			mDirectoryContent = response.getDirContent();
			updateView(response.getDirContent());

		}
	}
}
