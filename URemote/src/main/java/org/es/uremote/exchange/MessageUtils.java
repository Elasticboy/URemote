package org.es.uremote.exchange;

import org.es.uremote.exchange.Message.Request;
import org.es.uremote.exchange.Message.Request.Code;
import org.es.uremote.exchange.Message.Request.Type;
import org.es.uremote.exchange.Message.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.es.uremote.exchange.Message.Request.Code.NONE;

/**
 * Class that holds the utils to manage network messages
 *
 * @author Cyril Leroux
 * Created on 15/05/13.
 */
public class MessageUtils {

	/**
	 * Build a request with both integer and string parameters.
	 *
	 * @param securityToken The security token that identify the user.
	 * @param type The request type.
	 * @param code The request code.
	 * @param extraCode The request extra code.
	 * @param intExtra An integer parameter.
	 * @param stringExtra A string parameter.
	 * @return The request if it had been initialized. Return null otherwise.
	 */
	private static Request buildRequest(final String securityToken, final Type type, final Code code, final Code extraCode, final int intExtra, final String stringExtra) {

		Request request = Request.newBuilder()
				.setSecurityToken(securityToken)
				.setType(type)
				.setCode(code)
				.setExtraCode(extraCode)
				.setIntExtra(intExtra)
				.setStringExtra(stringExtra)
				.build();

		if (request.isInitialized()) {
			return request;
		}
		return null;
	}

	/**
	 * Build a request with an integer parameter.
	 *
	 * @param securityToken The security token that identify the user.
	 * @param type The request type.
	 * @param code The request code.
	 * @param intExtra An integer parameter.
	 * @return The request if it had been initialized. Return null otherwise.
	 */
	public static Request buildRequest(final String securityToken, final Type type, final Code code, final int intExtra) {
		return buildRequest(securityToken, type, code, NONE, intExtra, "");
	}

	/**
	 * Build a request with a string parameter.
	 *
	 * @param securityToken The security token that identify the user.
	 * @param type The request type.
	 * @param code The request code.
	 * @param extraCode The request extra code.
	 * @param stringExtra A string parameter.
	 * @return The request if it had been initialized. Return null otherwise.
	 */
	public static Request buildRequest(final String securityToken, final Type type, final Code code, final Code extraCode, final String stringExtra) {
		return buildRequest(securityToken, type, code, extraCode, 0, stringExtra);
	}

	/**
	 * Build a request with a code and an extra code.
	 *
	 * @param securityToken The security token that identify the user.
	 * @param type The request type.
	 * @param code The request code.
	 * @param extraCode The request extra code.
	 * @return The request if it had been initialized. Return null otherwise.
	 */
	public static Request buildRequest(final String securityToken, final Type type, final Code code, final Code extraCode) {
		return buildRequest(securityToken, type, code, extraCode, 0, "");
	}

	/**
	 * Build a request with a code.
	 *
	 * @param securityToken The security token that identify the user.
	 * @param type The request type.
	 * @param code The request code.
	 * @return The request if it had been initialized. Return null otherwise.
	 */
	public static Request buildRequest(final String securityToken, final Type type, final Code code) {
		return buildRequest(securityToken, type, code, NONE, 0, "");
	}

	/**
	 * This function must be called from a background thread.
	 * Send a message through a Socket to a server and get the reply.
	 *
	 * @param socket The socket on which to send the message.
	 * @param request Client request.
	 * @return The server reply.
	 *
	 * @throws IOException exception.
	 */
	public static Response sendRequest(Socket socket, Request request) throws IOException {
		if (socket.isConnected()) {
			//create BAOS for protobuf
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			//mClientDetails is a protobuf message object, dump it to the BAOS
			request.writeDelimitedTo(baos);

			socket.getOutputStream().write(baos.toByteArray());
			socket.getOutputStream().flush();
			socket.shutdownOutput();

			return Response.parseDelimitedFrom(socket.getInputStream());
		}
		return null;
	}
}
