package org.prelle.realmrunner.web.views.home;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import org.prelle.realmrunner.web.components.avataritem.AvatarItem;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Home")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.PENCIL_RULER_SOLID)
@AnonymousAllowed
public class HomeView extends Composite<VerticalLayout> {

    public HomeView() {
        FormLayout formLayout3Col = new FormLayout();
        AvatarItem avatarItem = new AvatarItem();
        AvatarItem avatarItem2 = new AvatarItem();
        AvatarItem avatarItem3 = new AvatarItem();
        getContent().setWidth("100%");
        getContent().getStyle().set("flex-grow", "1");
        formLayout3Col.setWidth("100%");
        formLayout3Col.setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("250px", 2),
                new ResponsiveStep("500px", 3));
        avatarItem.setWidth("min-content");
        setAvatarItemSampleData(avatarItem);
        avatarItem2.setWidth("min-content");
        setAvatarItemSampleData(avatarItem2);
        avatarItem3.setWidth("min-content");
        setAvatarItemSampleData(avatarItem3);
        getContent().add(formLayout3Col);
        getContent().add(avatarItem);
        getContent().add(avatarItem2);
        getContent().add(avatarItem3);
    }

    private void setAvatarItemSampleData(AvatarItem avatarItem) {
        avatarItem.setHeading("Aria Bailey");
        avatarItem.setDescription("Endocrinologist");
        avatarItem.setAvatar(new Avatar("Aria Bailey"));
    }
}
