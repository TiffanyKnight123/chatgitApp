package io.chatgitapp.email;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import io.chatgitapp.emaillist.EmailListItem;
import io.chatgitapp.emaillist.EmailListItemKey;
import io.chatgitapp.emaillist.EmailListItemRepository;
import io.chatgitapp.folders.UnreadEmailStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Autowired private EmailRepository emailRepository;

    @Autowired private EmailListItemRepository emailListItemRepository;

    @Autowired private UnreadEmailStatsRepository unreadEmailStatsRepository;


    public void sendEmail(String from, List<String> to, String subject, String body){
        Email email = new Email();
        email.setTo(to);
        email.setFrom(from);
        email.setSubject(subject);
        email.setBody(body);
        email.setId(Uuids.timeBased());
        emailRepository.save(email);

        to.forEach(toId -> {
            EmailListItem item = createEmailListItem(to, subject, email, toId, "Inbox");
            emailListItemRepository.save(item);
            unreadEmailStatsRepository.incrementUnreadCounter(toId, "Inbox");
        });

        EmailListItem sentItemEntry = createEmailListItem(to, subject, email, from, "Sent");
        sentItemEntry.setUnread(false);
        emailListItemRepository.save(sentItemEntry);
    }

    private EmailListItem createEmailListItem(List<String> to, String subject, Email email, String itemOwner, String folder) {
        EmailListItemKey key = new EmailListItemKey();
        key.setId(itemOwner);
        key.setLabel(folder);
        key.setTimeUUID(email.getId());
        EmailListItem item = new EmailListItem();
        item.setKey(key);
        item.setTo(to);
        item.setSubject(subject);
        item.setUnread(true);
        return item;
    }

    public boolean doesHaveAccess(Email email, String userId){
        return (userId.equals(email.getFrom()) || !email.getTo().contains(userId));
    }

    public String getReplySubject(String subject){
        return "Re: " + subject;
    }

    public String getReplyBody(Email email){
        return "\n\n\n-------------------------\n" +
                "From: " + email.getFrom() + "\n" +
                "To: " + email.getTo() + "\n\n" +
                email.getBody();
    }
}
