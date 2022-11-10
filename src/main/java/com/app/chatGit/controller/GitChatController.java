package io.chatgitapp.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import io.chatgitapp.email.EmailRepository;
import io.chatgitapp.emaillist.EmailListItem;
import io.chatgitapp.emaillist.EmailListItemRepository;
import io.chatgitapp.folders.*;
import org.checkerframework.checker.units.qual.A;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class GitChatController {

    @Autowired private FolderRepository folderRepository;

    @Autowired private FolderService folderService;

    @Autowired private EmailListItemRepository emailListItemRepository;


    @GetMapping(value = "/")
    public String homePage(
            @RequestParam(required = false) String folder,
            @AuthenticationPrincipal OAuth2User principal,
            Model model)
    {
        if(principal == null || !StringUtils.hasText(principal.getAttribute("login"))){
            return "index";
        }

        String userId = principal.getAttribute("login");
        //fetch folders
        List<Folder> userFolders = folderRepository.findAllById(userId);
        model.addAttribute("userFolders", userFolders);
        List<Folder> defaultFolders = folderService.fetchDefaultFolders(userId);
        model.addAttribute("defaultFolders", defaultFolders);
        model.addAttribute("stats", folderService.mapCountToLabels(userId));
        model.addAttribute("userName", principal.getAttribute("name"));

        //fetch messages
        if(!StringUtils.hasText((folder))){
            folder = "Inbox";
        }
        List<EmailListItem> emailList = emailListItemRepository
                .findAllByKey_IdAndKey_label(userId, folder);
        PrettyTime p = new PrettyTime();
        emailList.stream().forEach(emailListItem -> {
            UUID timeUuid = emailListItem.getKey().getTimeUUID();
            Date date = new Date(Uuids.unixTimestamp(timeUuid));
            emailListItem.setAgoTimeString(p.format(date));
        });
        model.addAttribute("emailList", emailList);
        model.addAttribute("folderName", folder);
        return "home-page";
    }
}
