package me.rafaskb.ticketmaster.commands;

import me.rafaskb.ticketmaster.models.Ticket;
import me.rafaskb.ticketmaster.sql.Controller;
import me.rafaskb.ticketmaster.utils.CooldownManager;
import me.rafaskb.ticketmaster.utils.CooldownManager.CooldownType;
import me.rafaskb.ticketmaster.utils.Lang;
import me.rafaskb.ticketmaster.utils.LangMacro;
import me.rafaskb.ticketmaster.utils.Perm;
import me.rafaskb.ticketmaster.utils.Utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandNew extends Command {
	private static final int INDEX_MESSAGE_START = 1;
	
	public CommandNew() {
		super(Perm.USER_CREATE);
	}
	
	@Override
	protected void run(CommandSender sender, String[] args) {
		// Not a player
		if(!(sender instanceof Player)) {
			Lang.sendErrorMessage(sender, Lang.CANNOT_RUN_FROM_CONSOLE);
			return;
		}
		Player player = ((Player) sender);
		
		// If not enough arguments
		if(args.length < 2) {
			Lang.sendErrorMessage(sender, Lang.NEW_COMMAND_USAGE);
			return;
		}
		
		// Check cooldown
		if(CooldownManager.isUnderCooldown(player.getName(), CooldownType.NEW_TICKET)) {
			Lang.sendErrorMessage(sender, Lang.NEW_COMMAND_COOLDOWN);
			return;
		}
		
		// Create ticket
		String request = Utils.convertArgumentsToString(args, INDEX_MESSAGE_START);
		Ticket ticket = new Ticket(player, request);
		
		// Insert ticket to database
		boolean success = Controller.insertOrUpdateTicket(ticket);
		
		// If it worked, send messages
		if(success) {
			// Add cooldown
			CooldownManager.addCooldown(sender.getName(), CooldownType.NEW_TICKET);
			
			// Tell submitter
			String msgToSender = LangMacro.replaceId(Lang.NEW_COMMAND_SUCCESS, ticket.getId());
			Lang.sendMessage(sender, msgToSender);
			
			// Broadcast new ticket to online helpers
			broadcastTicketCreation(ticket);
		}
		
		// If it failed (should never happen), send failure message
		else {
			Lang.sendErrorMessage(sender, Lang.NEW_COMMAND_FAILURE);
		}
		
	}
	
	private void broadcastTicketCreation(Ticket ticket) {
		String perm = ticket.getPriority().getRequiredPermission();
		
		String msgToHelper = LangMacro.replaceId(Lang.NEW_TICKET_BROADCAST, ticket.getId());
		msgToHelper = LangMacro.replaceSubmitter(msgToHelper, ticket.getSubmitter());
		
		for(Player p : Utils.getOnlinePlayers())
			if(p.hasPermission(perm))
				Lang.sendMessage(p, msgToHelper);
	}
	
}
