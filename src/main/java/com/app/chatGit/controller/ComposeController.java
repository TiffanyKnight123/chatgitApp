
import io.chatgitapp.email.Email;
import io.chatgitapp.email.EmailRepository;
import io.chatgitapp.email.EmailService;
import io.chatgitapp.folders.Folder;
import io.chatgitapp.folders.FolderRepository;
import io.chatgitapp.folders.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ComposeController {
    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FolderService folderService;

    @Autowired private EmailService emailService;

    @Autowired private EmailRepository emailRepository;


    @GetMapping(value = "/compose")
    public String getComposePage(
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID id,
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
        model.addAttribute("userName", principal.getAttribute("name"));

        model.addAttribute("stats", folderService.mapCountToLabels(userId));
        List<String> uniqueToIds = toIds(to);
        model.addAttribute("toIds", String.join(", ", uniqueToIds));

        if(id != null) {
            Optional<Email> optionEmail = emailRepository.findById(id);
            if(optionEmail.isPresent()){
                Email email = optionEmail.get();
                String toIds = String.join(", ", email.getTo());
                if(emailService.doesHaveAccess(email, userId)){
                    model.addAttribute("subject", emailService.getReplySubject(email.getSubject()));
                    model.addAttribute("body", emailService.getReplyBody(email));
                }
            }
        }


        return "compose-page";
    }

    @PostMapping("/sendEmail")
    public ModelAndView sendMessage(
            @RequestBody MultiValueMap<String, String> formData,
            @AuthenticationPrincipal OAuth2User principal
    ){
        if (principal == null || !StringUtils.hasText(principal.getAttribute("login")))
            return new ModelAndView("redirect:/");
        String from = principal.getAttribute("login");
        List<String> toIds = toIds(formData.getFirst("toIds"));
        String subject = formData.getFirst("subject");
        String body = formData.getFirst("body");
        emailService.sendEmail(from, toIds, subject, body);
        return new ModelAndView("redirect:/");
    }

    private List<String> toIds(String to) {
        if(!StringUtils.hasText(to))
            return new ArrayList<>();

        String[] splitIds = to.split(",");
        return Arrays.stream(splitIds)
                .map(StringUtils::trimWhitespace)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }
}
