/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.social.webui.space;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.ModelObject;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.Visibility;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.webui.page.UIPageSelector;
import org.exoplatform.portal.webui.page.UIWizardPageSetInfo;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormInputIconSelector;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTabPane;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.validator.DateTimeValidator;
import org.exoplatform.webui.form.validator.IdentifierValidator;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.StringLengthValidator;
import org.exoplatform.webui.form.validator.Validator;

public class UIPageNodeForm extends UIFormTabPane {

  private TreeNode                     pageNode_;

  private Object                       selectedParent;

  /**
   * PageNavigation to which editted PageNode belongs
   */
  private UserNavigation               contextPageNavigation;

  final private static String          SHOW_PUBLICATION_DATE  = "showPublicationDate";

  final private static String          START_PUBLICATION_DATE = "startPublicationDate";

  final private static String          END_PUBLICATION_DATE   = "endPublicationDate";

  final private static String          VISIBLE                = "visible";

  private Map<String, Described.State> cachedLabels;

  private String                       selectedLocale;

  private static final String          I18N_LABEL             = "i18nizedLabel";

  private static final String          LANGUAGES              = "languages";

  private static final String          LANGUAGES_ONCHANGE     = "ChangeLanguage";

  private static final String          SWITCH_MODE            = "switchmode";

  private static final String          SWITCH_MODE_ONCHANGE   = "SwitchLabelMode";

  private static final String          LABEL                  = "label";
  
  public UIPageNodeForm() throws Exception {
    super("UIPageNodeForm");

    UIFormInputSet uiSettingSet = new UIFormInputSet("PageNodeSetting");

    UICheckBoxInput uiDateInputCheck = new UICheckBoxInput(SHOW_PUBLICATION_DATE, null, false);
    UICheckBoxInput uiVisibleCheck = new UICheckBoxInput(VISIBLE, null, true);
    UICheckBoxInput uiSwitchLabelMode = new UICheckBoxInput(SWITCH_MODE, null, true);
    
    uiDateInputCheck.setOnChange("SwitchPublicationDate");
    uiVisibleCheck.setOnChange("SwitchVisible");
    uiSwitchLabelMode.setOnChange(SWITCH_MODE_ONCHANGE);

    UIFormSelectBox uiFormLanguagesSelectBox = new UIFormSelectBox(LANGUAGES, null, null);
    initLanguageSelectBox(uiFormLanguagesSelectBox);
    uiFormLanguagesSelectBox.setOnChange(LANGUAGES_ONCHANGE);

    uiSettingSet.addUIFormInput(new UIFormStringInput("URI", "URI", null).setDisabled(true))
                .addUIFormInput(new UIFormStringInput("name", "name", null).addValidator(MandatoryValidator.class)
                                                                           .addValidator(StringLengthValidator.class,
                                                                                         3,
                                                                                         30)
                                                                           .addValidator(IdentifierValidator.class))
                .addUIFormInput(uiSwitchLabelMode)
                .addUIFormInput(new UIFormStringInput(LABEL, LABEL, null).addValidator(StringLengthValidator.class, 
                                                                                       3,
                                                                                       120))
                .addUIFormInput(uiFormLanguagesSelectBox)
                .addUIFormInput(new UIFormStringInput(I18N_LABEL, null, null).setMaxLength(255)
                                                                             .addValidator(StringLengthValidator.class, 
                                                                                           3,
                                                                                           120))
                .addUIFormInput(uiVisibleCheck.setChecked(true))
                .addUIFormInput(uiDateInputCheck)
                .addUIFormInput(new UIFormDateTimeInput(START_PUBLICATION_DATE, null, null).addValidator(DateTimeValidator.class))
                .addUIFormInput(new UIFormDateTimeInput(END_PUBLICATION_DATE, null, null).addValidator(DateTimeValidator.class));

    addUIFormInput(uiSettingSet);
    setSelectedTab(uiSettingSet.getId());

    UIPageSelector uiPageSelector = createUIComponent(UIPageSelector.class, null, null);
    uiPageSelector.configure("UIPageSelector", "pageRef");
    addUIFormInput(uiPageSelector);

    UIFormInputIconSelector uiIconSelector = new UIFormInputIconSelector("Icon", "icon");
    addUIFormInput(uiIconSelector);
    setActions(new String[] { "Save", "Back" });
  }

  public TreeNode getPageNode() {
    return pageNode_;
  }

  public void setValues(TreeNode pageNode) throws Exception {
    pageNode_ = pageNode;
    selectedLocale = getUIFormSelectBox(LANGUAGES).getValue();
    cachedLabels = new HashMap<String, Described.State>();
    if (pageNode == null) {
      getUIStringInput("name").setReadOnly(false);
      getChild(UIFormInputIconSelector.class).setSelectedIcon("Default");
      setShowPublicationDate(false);
      switchLabelMode(true);
      return;
    }
    getUIStringInput("name").setReadOnly(true);
    invokeGetBindingBean(pageNode_);
  }

  private void initLanguageSelectBox(UIFormSelectBox langSelectBox) {
    List<SelectItemOption<String>> lang = new ArrayList<SelectItemOption<String>>();
    LocaleConfigService localeService = getApplicationComponent(LocaleConfigService.class);
    Locale currentLocale = ((PortletRequestContext) WebuiRequestContext.getCurrentInstance()).getLocale();
    Iterator<LocaleConfig> i = localeService.getLocalConfigs().iterator();
    String displayName = null;
    String language = null;
    String country = null;
    String defaultValue = null;
    SelectItemOption<String> option;
    while (i.hasNext()) {
      LocaleConfig config = i.next();
      Locale locale = config.getLocale();

      language = locale.getLanguage();
      country = locale.getCountry();
      if (country != null && country.length() > 0) {
        language = language + "_" + country;
      }

      ResourceBundle localeResourceBundle;

      displayName = null;
      try {
        localeResourceBundle = getResourceBundle(currentLocale);
        String key = "Locale." + language;
        String translation = localeResourceBundle.getString(key);
        displayName = translation;
      } catch (MissingResourceException e) {
        displayName = capitalizeFirstLetter(locale.getDisplayName(currentLocale));
      } catch (Exception e) {

      }

      option = new SelectItemOption<String>(displayName, language);
      if (locale.getDisplayName().equals(currentLocale.getDisplayName())) {
        option.setSelected(true);
        defaultValue = language;
      }

      lang.add(option);
    }

    Collections.sort(lang, new LanguagesComparator());
    langSelectBox.setOptions(lang);
    langSelectBox.setValue(defaultValue);
  }

  private ResourceBundle getResourceBundle(Locale locale) throws Exception {
    ExoContainer appContainer = ExoContainerContext.getCurrentContainer();
    ResourceBundleService service = (ResourceBundleService) appContainer.getComponentInstanceOfType(ResourceBundleService.class);
    ResourceBundle res = service.getResourceBundle("locale.portal.webui", locale);
    return res;
  }

  private String capitalizeFirstLetter(String word) {
    if (word == null) {
      return null;
    }
    if (word.length() == 0) {
      return word;
    }
    StringBuilder result = new StringBuilder(word);
    result.replace(0, 1, result.substring(0, 1).toUpperCase());
    return result.toString();
  }

  private class LanguagesComparator implements Comparator<SelectItemOption<String>> {
    public int compare(SelectItemOption<String> o1, SelectItemOption<String> o2) {
      return o1.getLabel().compareToIgnoreCase(o2.getLabel());
    }
  }

  public void invokeGetBindingBean(Object bean) throws Exception {
    super.invokeGetBindingBean(bean);

    TreeNode pageNode = (TreeNode) bean;

    String icon = pageNode.getIcon();
    if (icon == null || icon.length() < 0)
      icon = "Default";
    getChild(UIFormInputIconSelector.class).setSelectedIcon(icon);
    getUIStringInput(LABEL).setValue(pageNode.getLabel());
    Map<Locale, Described.State> i18nizedLabels = pageNode.getI18nizedLabels();
    if (i18nizedLabels != null) {
      for (Locale key : i18nizedLabels.keySet()) {
        String locale = key.getCountry() != "" ? key.getLanguage() + "_" + key.getCountry()
                                               : key.getLanguage(); 
        cachedLabels.put(locale, i18nizedLabels.get(key));
      }
    }

    if (cachedLabels.get(selectedLocale) != null) {
      getUIStringInput(I18N_LABEL).setValue(cachedLabels.get(selectedLocale).getName());
    }

    if (pageNode.getVisibility() == Visibility.SYSTEM) {
      UIFormInputSet uiSettingSet = getChildById("PageNodeSetting");
      uiSettingSet.removeChildById(VISIBLE);
      uiSettingSet.removeChildById(SHOW_PUBLICATION_DATE);
      uiSettingSet.removeChildById(START_PUBLICATION_DATE);
      uiSettingSet.removeChildById(END_PUBLICATION_DATE);
    } else {
      Visibility visibility = pageNode.getVisibility();
      boolean isVisible = visibility == null
          || EnumSet.of(Visibility.DISPLAYED, Visibility.TEMPORAL).contains(visibility);
      getUICheckBoxInput(VISIBLE).setChecked(isVisible);
      getUICheckBoxInput(SHOW_PUBLICATION_DATE).setChecked(Visibility.TEMPORAL.equals(visibility));
      setShowCheckPublicationDate(isVisible);
      Calendar cal = Calendar.getInstance();
      if (pageNode.getStartPublicationTime() != -1) {
        cal.setTime(new Date(pageNode.getStartPublicationTime()));
        getUIFormDateTimeInput(START_PUBLICATION_DATE).setCalendar(cal);
      } else
        getUIFormDateTimeInput(START_PUBLICATION_DATE).setValue(null);
      if (pageNode.getEndPublicationTime() != -1) {
        cal.setTime(new Date(pageNode.getEndPublicationTime()));
        getUIFormDateTimeInput(END_PUBLICATION_DATE).setCalendar(cal);
      } else
        getUIFormDateTimeInput(END_PUBLICATION_DATE).setValue(null);
    }

    boolean isExtendedMode = true;
    if (pageNode.getNode().getLabel() != null && pageNode.getNode().getLabel().trim().length() > 0) {
      isExtendedMode = false;
    }

    getUICheckBoxInput(SWITCH_MODE).setChecked(isExtendedMode);
    this.switchLabelMode(isExtendedMode);
  }

  public void invokeSetBindingBean(Object bean) throws Exception {
    super.invokeSetBindingBean(bean);
    TreeNode node = (TreeNode) bean;

    if (node.getVisibility() != Visibility.SYSTEM) {
      Visibility visibility;
      if (getUICheckBoxInput(VISIBLE).isChecked()) {
        UICheckBoxInput showPubDate = getUICheckBoxInput(SHOW_PUBLICATION_DATE); 
        visibility = showPubDate.isChecked() ? Visibility.TEMPORAL : Visibility.DISPLAYED;
      } else {
        visibility = Visibility.HIDDEN;
      }
      node.setVisibility(visibility);

      Calendar cal = getUIFormDateTimeInput(START_PUBLICATION_DATE).getCalendar();
      Date date = (cal != null) ? cal.getTime() : null;
      node.setStartPublicationTime(date == null ? -1 : date.getTime());
      cal = getUIFormDateTimeInput(END_PUBLICATION_DATE).getCalendar();
      date = (cal != null) ? cal.getTime() : null;
      node.setEndPublicationTime(date == null ? -1 : date.getTime());
    }

    cachedLabels.put(getUIFormSelectBox(LANGUAGES).getValue(),
                     new Described.State(getUIStringInput(I18N_LABEL).getValue(), null));
    Map<Locale, Described.State> labels = new HashMap<Locale, Described.State>(cachedLabels.size());
    getUIFormSelectBox(LANGUAGES).getValue();
    for (String strLocale : cachedLabels.keySet()) {
      Locale locale;
      if (strLocale.contains("_")) {
        String[] arr = strLocale.split("_");
        if (arr.length > 2) {
          locale = new Locale(arr[0], arr[1], arr[2]);
        } else {
          locale = new Locale(arr[0], arr[1]);
        }
      } else {
        locale = new Locale(strLocale);
      }

      labels.put(locale, cachedLabels.get(strLocale));
    }

    node.setI18nizedLabels(labels);

    if (getUICheckBoxInput(SWITCH_MODE).getValue().toString().equals("true"))
      node.setLabel(null);
    else if (node.getLabel() == null)
      node.setLabel(node.getName());
  }

  public void setShowCheckPublicationDate(boolean show) {
    getUICheckBoxInput(VISIBLE).setChecked(show);
    UICheckBoxInput uiForm = getUICheckBoxInput(SHOW_PUBLICATION_DATE);
    uiForm.setRendered(show);
    setShowPublicationDate(show && uiForm.isChecked());
  }

  public void setShowPublicationDate(boolean show) {
    getUIFormDateTimeInput(START_PUBLICATION_DATE).setRendered(show);
    getUIFormDateTimeInput(END_PUBLICATION_DATE).setRendered(show);
  }

  public Object getSelectedParent() {
    return selectedParent;
  }

  public void setSelectedParent(Object obj) {
    this.selectedParent = obj;
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    super.processRender(context);

    UIPageSelector uiPageSelector = getChild(UIPageSelector.class);
    if (uiPageSelector == null)
      return;
    UIPopupWindow uiPopupWindowPage = uiPageSelector.getChild(UIPopupWindow.class);
    if (uiPopupWindowPage == null)
      return;
    uiPopupWindowPage.processRender(context);
  }

  public String getOwner() {
    return contextPageNavigation.getKey().getName();
  }

  public SiteType getOwnerType() {
    return contextPageNavigation.getKey().getType();
  }

  public void setContextPageNavigation(UserNavigation _contextPageNav) {
    this.contextPageNavigation = _contextPageNav;
  }

  public UserNavigation getContextPageNavigation() {
    return this.contextPageNavigation;
  }

  static public class SaveActionListener extends EventListener<UIPageNodeForm> {
    public void execute(Event<UIPageNodeForm> event) throws Exception {
      WebuiRequestContext ctx = event.getRequestContext();
      UIPageNodeForm uiPageNodeForm = event.getSource();
      UserNavigation contextNavigation = uiPageNodeForm.getContextPageNavigation();
      UIApplication uiPortalApp = ctx.getUIApplication();
      TreeNode pageNode = uiPageNodeForm.getPageNode();

      if (pageNode == null
          || (pageNode.getVisibility() != Visibility.SYSTEM && uiPageNodeForm.getUICheckBoxInput(SHOW_PUBLICATION_DATE)
                                                                             .isChecked())) {
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.set(currentCalendar.get(Calendar.YEAR),
                            currentCalendar.get(Calendar.MONTH),
                            currentCalendar.get(Calendar.DAY_OF_MONTH),
                            0,
                            0,
                            0);
        Date currentDate = currentCalendar.getTime();

        Calendar startCalendar = uiPageNodeForm.getUIFormDateTimeInput(UIWizardPageSetInfo.START_PUBLICATION_DATE)
                                               .getCalendar();
        Date startDate = startCalendar != null ? startCalendar.getTime() : currentDate;
        Calendar endCalendar = uiPageNodeForm.getUIFormDateTimeInput(UIWizardPageSetInfo.END_PUBLICATION_DATE)
                                             .getCalendar();
        Date endDate = endCalendar != null ? endCalendar.getTime() : null;

        // Case 1: current date after start date
        if (currentDate.after(startDate)) {
          Object[] args = {};
          uiPortalApp.addMessage(new ApplicationMessage("UIPageNodeForm.msg.currentDateBeforeStartDate",
                                                        args,
                                                        ApplicationMessage.WARNING));
          return;
        }
        // Case 2: start date after end date
        else if ((endCalendar != null) && (startCalendar != null) && (startDate.after(endDate))) {
          Object[] args = {};
          uiPortalApp.addMessage(new ApplicationMessage("UIPageNodeForm.msg.startDateBeforeEndDate",
                                                        args,
                                                        ApplicationMessage.WARNING));
          return;
        }
        // Case 3: start date is null and current date after end date
        else if ((endCalendar != null) && (currentDate.after(endDate))) {
          Object[] args = {};
          uiPortalApp.addMessage(new ApplicationMessage("UIPageNodeForm.msg.currentDateBeforeEndDate",
                                                        args,
                                                        ApplicationMessage.WARNING));
          return;
        }

      }

      UIFormStringInput nameInput = uiPageNodeForm.getUIStringInput("name");
      String nodeName = nameInput.getValue();

      TreeNode selectedParent = (TreeNode) uiPageNodeForm.getSelectedParent();
      if (pageNode == null && selectedParent.getChild(nodeName) != null) {
        uiPortalApp.addMessage(new ApplicationMessage("UIPageNodeForm.msg.SameName", null));
        return;
      }

      // Add node that need to be rebased to context
      if (pageNode == null) {
        pageNode = selectedParent.addChild(nodeName);
        uiPageNodeForm.pageNode_ = pageNode;
      }

      UIPageSelector pageSelector = uiPageNodeForm.getChild(UIPageSelector.class);
      if (pageSelector.getPage() == null) {
        pageSelector.setValue(null);
      } else {
        PageContext pageContext = pageSelector.getPage();
        DataStorage storage = uiPageNodeForm.getApplicationComponent(DataStorage.class);
        PageService pageService = uiPageNodeForm.getApplicationComponent(PageService.class);
        if (pageService.loadPage(pageContext.getKey()) == null) {
            pageService.savePage(pageContext);

            //
            Page page = new Page();
            page.setOwnerType(pageContext.getKey().getSite().getTypeName());
            page.setOwnerId(pageContext.getKey().getSite().getName());
            page.setName(pageContext.getKey().getName());
            String title = pageContext.getState().getDisplayName();
            String[] accessPermission = pageContext.getState().getAccessPermissions() == null ? null : pageContext
                    .getState().getAccessPermissions()
                    .toArray(new String[pageContext.getState().getAccessPermissions().size()]);
            if (title == null || title.trim().length() < 1) {
                title = page.getName();
            }
            page.setTitle(title);
            page.setShowMaxWindow(false);
            page.setAccessPermissions(accessPermission);
            page.setEditPermission(pageContext.getState().getEditPermission());
            page.setModifiable(true);
            if (page.getChildren() == null) {
                page.setChildren(new ArrayList<ModelObject>());
            }

            storage.save(page);
            pageSelector.setValue(page.getPageId());
        }
      }

      if (pageNode.getLabel() == null)
        pageNode.setLabel(pageNode.getName());
      uiPageNodeForm.invokeSetBindingBean(pageNode);

      UIFormInputIconSelector uiIconSelector = uiPageNodeForm.getChild(UIFormInputIconSelector.class);
      if (uiIconSelector.getSelectedIcon().equals("Default"))
        pageNode.setIcon(null);
      else
        pageNode.setIcon(uiIconSelector.getSelectedIcon());

      UIPopupWindow uiPopup = uiPageNodeForm.getParent();
      UISpaceNavigationManagement uiSpaceNav = uiPopup.getParent();
      UISpaceNavigationNodeSelector uiNodeSelector = uiSpaceNav.getChild(UISpaceNavigationNodeSelector.class);
      if (pageNode != null) {
        uiNodeSelector.getUserNodeLabels().put(pageNode.getId(), pageNode.getI18nizedLabels());
      } 
      uiNodeSelector.save();
      uiSpaceNav.setOwner(contextNavigation.getKey().getName());
      uiSpaceNav.setOwnerType(contextNavigation.getKey().getTypeName());
      uiNodeSelector.setEdittedNavigation(contextNavigation);
      uiNodeSelector.initTreeData();
      uiPopup.setShow(false);
      SpaceUtils.updateWorkingWorkSpace();
    }
  }

  static public class BackActionListener extends EventListener<UIPageNodeForm> {

    public void execute(Event<UIPageNodeForm> event) throws Exception {

    }
  }

  public static class ChangeLanguageActionListener extends EventListener<UIPageNodeForm> {
    @Override
    public void execute(Event<UIPageNodeForm> event) throws Exception {
      UIPageNodeForm uiForm = event.getSource();
      UIFormSelectBox languageSelection = uiForm.getUIFormSelectBox(LANGUAGES);
      UIFormStringInput label = uiForm.getUIStringInput(I18N_LABEL);
      uiForm.updateCachedLabels(uiForm.getSelectedLocale(), label.getValue());

      uiForm.setSelectedLocale(languageSelection.getValue());
      label.setValue(uiForm.getLabelOnLocale(uiForm.getSelectedLocale()));
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm); 
    }
  }

  private String getLabelOnLocale(String locale) {
    if (cachedLabels.get(locale) != null) {
      return cachedLabels.get(locale).getName();
    }

    return null;
  }

  private void updateCachedLabels(String locale, String label) {
    cachedLabels.put(locale, new Described.State(label, null));
  }

  public void setSelectedLocale(String selectedLocale) {
    this.selectedLocale = selectedLocale;
  }

  public String getSelectedLocale() {
    return selectedLocale;
  }

  static public class SwitchPublicationDateActionListener extends EventListener<UIPageNodeForm> {
    public void execute(Event<UIPageNodeForm> event) throws Exception {
      UIPageNodeForm uiForm = event.getSource();
      boolean isCheck = uiForm.getUICheckBoxInput(SHOW_PUBLICATION_DATE).isChecked();
      uiForm.setShowPublicationDate(isCheck);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  static public class SwitchVisibleActionListener extends EventListener<UIPageNodeForm> {
    @Override
    public void execute(Event<UIPageNodeForm> event) throws Exception {
      UIPageNodeForm uiForm = event.getSource();
      boolean isCheck = uiForm.getUICheckBoxInput(VISIBLE).isChecked();
      uiForm.setShowCheckPublicationDate(isCheck);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  static public class SwitchLabelModeActionListener extends EventListener<UIPageNodeForm> {
    @Override
    public void execute(Event<UIPageNodeForm> event) throws Exception {
      UIPageNodeForm uiForm = event.getSource();
      boolean isExtendedMode = uiForm.getUICheckBoxInput(SWITCH_MODE).isChecked();
      uiForm.switchLabelMode(isExtendedMode);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  static public class ClearPageActionListener extends EventListener<UIPageNodeForm> {
    public void execute(Event<UIPageNodeForm> event) throws Exception {
      UIPageNodeForm uiForm = event.getSource();
      UIPageSelector pageSelector = uiForm.findFirstComponentOfType(UIPageSelector.class);
      pageSelector.setPage(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(pageSelector);
    }
  }

  static public class CreatePageActionListener extends EventListener<UIPageNodeForm> {
    public void execute(Event<UIPageNodeForm> event) throws Exception {
      UIPageNodeForm uiForm = event.getSource();
      UIPageSelector pageSelector = uiForm.findFirstComponentOfType(UIPageSelector.class);

      PortalRequestContext pcontext = Util.getPortalRequestContext();
      UIPortalApplication uiPortalApp = Util.getUIPortalApplication();

      UIFormInputSet uiInputSet = pageSelector.getChild(UIFormInputSet.class);
      List<UIComponent> children = uiInputSet.getChildren();
      /*********************************************************************/
      for (UIComponent uiChild : children) {
        if (uiChild instanceof UIFormInputBase) {
          UIFormInputBase uiInput = (UIFormInputBase) uiChild;
          if (!uiInput.isValid())
            continue;
          List<Validator> validators = uiInput.getValidators();
          if (validators == null)
            continue;
          try {
            for (Validator validator : validators)
              validator.validate(uiInput);
          } catch (MessageException ex) {
            uiPortalApp.addMessage(ex.getDetailMessage());
            return;
          } catch (Exception ex) {
            // TODO: This is a critical exception and should be handle in the
            // UIApplication
            uiPortalApp.addMessage(new ApplicationMessage(ex.getMessage(), null));
            return;
          }
        }
      }

      UserACL userACL = uiForm.getApplicationComponent(UserACL.class);

      String ownerId = uiForm.getOwner();
      String[] accessPermission = new String[1];
      accessPermission[0] = "*:" + ownerId;
      String editPermission = userACL.getMakableMT() + ":" + ownerId;

      if (SiteType.PORTAL.equals(uiForm.getOwnerType())) {
        UIPortal uiPortal = Util.getUIPortal();
        accessPermission = uiPortal.getAccessPermissions();
        editPermission = uiPortal.getEditPermission();
      }

      UIFormStringInput uiPageName = uiInputSet.getChildById("pageName");
      UIFormStringInput uiPageTitle = uiInputSet.getChildById("pageTitle");

      Page page = new Page();
      page.setOwnerType(uiForm.getOwnerType().getName());
      page.setOwnerId(ownerId);
      page.setName(uiPageName.getValue());
      String title = uiPageTitle.getValue();
      if (title == null || title.trim().length() < 1)
        title = page.getName();
      page.setTitle(title);

      page.setShowMaxWindow(false);

      page.setAccessPermissions(accessPermission);
      page.setEditPermission(editPermission);

      userACL.hasPermission(page);

      page.setModifiable(true);
      if (page.getChildren() == null)
        page.setChildren(new ArrayList<ModelObject>());

      PageState pageState = new PageState(uiPageTitle.getValue(), null, false, null,
              accessPermission != null ? Arrays.asList(accessPermission) : null, editPermission);
      
      // check page is exist
      PageKey pageKey = PageKey.parse(uiForm.getOwnerType().getName() + "::" + ownerId + "::" + uiPageName.getValue());
      PageService pageService = uiForm.getApplicationComponent(PageService.class);
      PageContext existPage = pageService.loadPage(pageKey);
      if (existPage != null) {
        uiPortalApp.addMessage(new ApplicationMessage("UIPageForm.msg.sameName", null));
        pcontext.addUIComponentToUpdateByAjax(uiPortalApp.getUIPopupMessages());
        return;
      }

      pageSelector.setPage(new PageContext(pageKey, pageState));
      event.getRequestContext().addUIComponentToUpdateByAjax(pageSelector); 
    }
  }

  private void switchLabelMode(boolean isExtendedMode) {
    getUIStringInput(LABEL).setRendered(!isExtendedMode);
    getUIStringInput(I18N_LABEL).setRendered(isExtendedMode);
    getUIFormSelectBox(LANGUAGES).setRendered(isExtendedMode);
  }
  
  static public class SelectTabActionListener extends UIFormTabPane.SelectTabActionListener {
    public void execute(Event<UIFormTabPane> event) throws Exception {
      super.execute(event);
      PortalRequestContext pcontext = Util.getPortalRequestContext();
      pcontext.setResponseComplete(true);
    }
  } 
}
