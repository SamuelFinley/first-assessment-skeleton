package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private Socket socket;
	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}
	
	
	public String userList() {
		String strng = "";
		for (String peeps : Cast()) {
			if (strng == "") {
				strng = peeps;
			} else {
				strng = strng + ":" + peeps;
			}
		}
		return strng;
	}
	
	public ArrayList<String> Cast() {
		ArrayList<String> cast = new ArrayList<String>();
		for (String peeps : Server.getClients().keySet()) {
			cast.add(peeps);
		}
		Collections.sort(cast);
		return cast;
	}
	
	public Socket comfySocks(String name) {
		Map<String, Socket> map = Server.getClients();
		Socket sock = map.get(name);
		return sock;
	}
	
	public String direct(Message msg) {
		Map<String, Socket> map = Server.getClients();
		String command = msg.getCommand();
		int bigs = command.length();
		String part = command.substring(1,bigs);
		msg.setCommand(command.substring(0, 1));
		map.containsKey(command);
		return part;
	}
	
	public static void cliTime(Message message) {
		LocalTime clk = LocalTime.now();
		String time = clk.toString();
		String[] times = time.split(Pattern.quote("."), 0);
		String hammerTime = times[0];
		message.setTime(hammerTime);
    }
	
	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				if (!Server.getClients().containsValue(socket) && Server.getClients().containsKey(message.getUsername())) {
					message.setCommand("username error");
					String sendAll = mapper.writeValueAsString(message);
					writer.write(sendAll);
					writer.flush();
					this.socket.close();
					break;
				}
				String name = new String();
				String first = message.getCommand().substring(0,1);
				cliTime(message);
				if (first.equals("@")) {
					name = direct(message);
				}
				switch (message.getCommand()) {
					case "disconnect":
						String discon = message.getUsername();
						Server.getClients().remove(message.getUsername());
						for (String clis : Cast()) {
							message.setContents(discon);
							Socket cozy = comfySocks(clis);
							PrintWriter newWriter = new PrintWriter(new OutputStreamWriter(cozy.getOutputStream()));
							String sendAll = mapper.writeValueAsString(message);
							newWriter.write(sendAll);
							newWriter.flush();
							log.info("<{}> user <{}> disconnected", message.getTime(), discon);
						}
						this.socket.close();
						break;
					case "echo":
						System.out.println(message.getTime());
						log.info("<{}> user <{}> echoed message: <{}>", message.getTime(), message.getUsername(), message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast":
						for (String clis : Cast()) {
							Socket cozy = comfySocks(clis);
							PrintWriter newWriter = new PrintWriter(new OutputStreamWriter(cozy.getOutputStream()));
							log.info("user <{}> broadcast message: <{}>", message.getUsername(), message.getContents());
							String sendAll = mapper.writeValueAsString(message);
							newWriter.write(sendAll);
							newWriter.flush();
						}
						break;
					case "users":
						System.out.println(userList());
						message.setContents(userList());
						log.info("user <{}>: users <{}>", message.getUsername(), message.getContents());
						String use = mapper.writeValueAsString(message);
						writer.write(use);
						writer.flush();
						break;
					case "@":
						if (Server.getClients().containsKey(name)) {
							Socket cozy = comfySocks(name);
							PrintWriter newWriter = new PrintWriter(new OutputStreamWriter(cozy.getOutputStream()));
							log.info("user <{}> (all): <{}>", message.getUsername(), message.getContents());
							String sendAll = mapper.writeValueAsString(message);
							newWriter.write(sendAll);
							newWriter.flush();
						} else {
							message.setContents("requested user is not connected");
							log.info("Command <@{}> was not recognized", message.getCommand());
							String err = mapper.writeValueAsString(message);
							writer.write(err);
							writer.flush();
						}
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
