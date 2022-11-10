package io.chatgitapp.controller;

import io.chatgitapp.email.Email;
import io.chatgitapp.email.EmailRepository;
import io.chatgitapp.email.EmailService;
import io.chatgitapp.emaillist.EmailListItem;
import io.chatgitapp.emaillist.EmailListItemKey;
import io.chatgitapp.emaillist.EmailListItemRepository;
import io.chatgitapp.folders.Folder;
import io.chatgitapp.folders.FolderRepository;
import io.chatgitapp.folders.FolderService;
import io.chatgitapp.folders.UnreadEmailStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class EmailViewController {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FolderService folderService;

    @Autowired
    private EmailListItemRepository emailListItemRepository;

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UnreadEmailStatsRepository unreadEmailStatsRepository;

    @GetMapping(value = "/messages/{id}")
    public String emailView(
            @RequestParam String folder,
            @PathVariable UUID id,
            @AuthenticationPrincipal OAuth2User principal,
            Model model)
    {
        if (principal == null || !StringUtils.hasText(principal.getAttribute("login")))
            return "index";

        String userId = principal.getAttribute("login");

        //fetch folders
        List<Folder> userFolders = folderRepository.findAllById(userId);
        model.addAttribute("userFolders", userFolders);
        List<Folder> defaultFolders = folderService.fetchDefaultFolders(userId);
        model.addAttribute("defaultFolders", defaultFolders);


        Optional<Email> optionEmail = emailRepository.findById(id);
        if(!optionEmail.isPresent())
            return "home-page";

        Email email = optionEmail.get();
        String toIds = String.join(", ", email.getTo());

        //Checking if user allowed to see the message
        if(!emailService.doesHaveAccess(email, userId)){
            return "redirect:/";
        }

        model.addAttribute("email", optionEmail.get());
        model.addAttribute("toIds", toIds);

        EmailListItemKey key = new EmailListItemKey();
        key.setId(userId);
        key.setLabel(folder);
        key.setTimeUUID(email.getId());

        Optional<EmailListItem> optionalEmailListItem = emailListItemRepository.findById(key);
        if(optionalEmailListItem.isPresent()){
            EmailListItem emailListItem = optionalEmailListItem.get();
            if(emailListItem.isUnread()){
                emailListItem.setUnread(false);
                emailListItemRepository.save(emailListItem);
                unreadEmailStatsRepository.decrementUnreadCounter(userId, folder);
            }
        }

        model.addAttribute("stats", folderService.mapCountToLabels(userId));
        model.addAttribute("userName", principal.getAttribute("name"));

        return "email-page";
    }
}
