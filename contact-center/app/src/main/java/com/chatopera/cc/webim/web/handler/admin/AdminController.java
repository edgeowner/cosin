/*
 * Copyright (C) 2017 优客服-多渠道客服系统
 * Modifications copyright (C) 2018 Chatopera Inc, <https://www.chatopera.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chatopera.cc.webim.web.handler.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.chatopera.cc.util.Menu;
import com.chatopera.cc.util.client.NettyClients;
import com.chatopera.cc.webim.service.cache.CacheHelper;
import com.chatopera.cc.webim.service.repository.OnlineUserRepository;
import com.chatopera.cc.webim.service.repository.SysDicRepository;
import com.chatopera.cc.webim.service.repository.UserEventRepository;
import com.chatopera.cc.webim.util.OnlineUserUtils;
import com.chatopera.cc.webim.web.model.SysDic;
import com.chatopera.cc.webim.web.model.User;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.chatopera.cc.core.UKDataContext;
import com.chatopera.cc.util.UKTools;
import com.chatopera.cc.webim.service.acd.ServiceQuene;
import com.chatopera.cc.webim.service.repository.UserRepository;
import com.chatopera.cc.webim.web.handler.Handler;

@Controller
public class AdminController extends Handler{

	@Autowired
	private UserRepository userRes;
	
	@Autowired
	private OnlineUserRepository onlineUserRes;
	
	@Autowired
	private UserEventRepository userEventRes ;
	
	@Autowired
	private SysDicRepository sysDicRes ;
	
    @RequestMapping("/admin")
    public ModelAndView index(ModelMap map ,HttpServletRequest request) {
    	ModelAndView view = request(super.createRequestPageTempletResponse("redirect:/"));
        User user = super.getUser(request) ;
        view.addObject("agentStatusReport",ServiceQuene.getAgentReport(user.getOrgi())) ;
		view.addObject("agentStatus", CacheHelper.getAgentStatusCacheBean().getCacheObject(user.getId(), user.getOrgi())) ;
        
		return view;
    }
    
    
    private void aggValues(ModelMap map , HttpServletRequest request){
    	map.put("onlineUserCache", CacheHelper.getOnlineUserCacheBean().getSize()) ;
    	map.put("onlineUserClients", OnlineUserUtils.webIMClients.size()) ;
    	map.put("chatClients", NettyClients.getInstance().size()) ;
    	map.put("systemCaches", CacheHelper.getSystemCacheBean().getSize()) ;
    	
		map.put("agentReport", ServiceQuene.getAgentReport(super.getOrgi(request))) ;
		map.put("webIMReport", UKTools.getWebIMReport(userEventRes.findByOrgiAndCreatetimeRange(super.getOrgi(request), UKTools.getStartTime() , UKTools.getEndTime()))) ;
		
		map.put("agents",getAgent(request).size()) ;

		map.put("webIMInvite", UKTools.getWebIMInviteStatus(onlineUserRes.findByOrgiAndStatus(super.getOrgi(request), UKDataContext.OnlineUserOperatorStatus.ONLINE.toString()))) ;
		
		map.put("inviteResult", UKTools.getWebIMInviteResult(onlineUserRes.findByOrgiAndAgentnoAndCreatetimeRange(super.getOrgi(request), super.getUser(request).getId() , UKTools.getStartTime() , UKTools.getEndTime()))) ;
		
		map.put("agentUserCount", onlineUserRes.countByAgentForAgentUser(super.getOrgi(request), UKDataContext.AgentUserStatusEnum.INSERVICE.toString(),super.getUser(request).getId() , UKTools.getStartTime() , UKTools.getEndTime())) ;
		
		map.put("agentServicesCount", onlineUserRes.countByAgentForAgentUser(super.getOrgi(request), UKDataContext.AgentUserStatusEnum.END.toString(),super.getUser(request).getId() , UKTools.getStartTime() , UKTools.getEndTime())) ;
		
		map.put("agentServicesAvg", onlineUserRes.countByAgentForAvagTime(super.getOrgi(request), UKDataContext.AgentUserStatusEnum.END.toString(),super.getUser(request).getId() , UKTools.getStartTime() , UKTools.getEndTime())) ;
		
		map.put("webInviteReport", UKTools.getWebIMInviteAgg(onlineUserRes.findByOrgiAndCreatetimeRange(super.getOrgi(request) , UKDataContext.ChannelTypeEnum.WEBIM.toString(),UKTools.getLast30Day(), UKTools.getEndTime()))) ;
		
		map.put("agentConsultReport", UKTools.getWebIMDataAgg(onlineUserRes.findByOrgiAndCreatetimeRangeForAgent(super.getOrgi(request), UKTools.getLast30Day(), UKTools.getEndTime()))) ;
		
		map.put("clentConsultReport", UKTools.getWebIMDataAgg(onlineUserRes.findByOrgiAndCreatetimeRangeForClient(super.getOrgi(request), UKTools.getLast30Day(), UKTools.getEndTime() , UKDataContext.ChannelTypeEnum.WEBIM.toString()))) ;
		
		map.put("browserConsultReport", UKTools.getWebIMDataAgg(onlineUserRes.findByOrgiAndCreatetimeRangeForBrowser(super.getOrgi(request), UKTools.getLast30Day(), UKTools.getEndTime(), UKDataContext.ChannelTypeEnum.WEBIM.toString()))) ;
	}
    private List<User> getAgent(HttpServletRequest request){
		//获取当前产品or租户坐席数
    	List<User> userList = new ArrayList<>();
		if(super.isEnabletneant()) {
			userList = userRes.findByOrgidAndAgentAndDatastatus(super.getOrgid(request), true, false);
		}else {
			 userList = userRes.findByOrgiAndAgentAndDatastatus(super.getOrgi(request), true, false);
		}
    	return userList.isEmpty()?new ArrayList<User>():userList;
	}
    
    @RequestMapping("/admin/content")
    @Menu(type = "admin" , subtype = "content")
    public ModelAndView content(ModelMap map , HttpServletRequest request) {
    	aggValues(map, request);
    	return request(super.createAdminTempletResponse("/admin/content"));
    	/*if(super.getUser(request).isSuperuser()) {
    		aggValues(map, request);
        	return request(super.createAdminTempletResponse("/admin/content"));
    	}else {
    		return request(super.createAdminTempletResponse("/admin/user/index"));
    	}*/
    }

    @RequestMapping("/admin/auth/infoacq")
    @Menu(type = "admin" , subtype = "infoacq" , access= false , admin = true)
    public ModelAndView infoacq(ModelMap map , HttpServletRequest request) {
    	String inacq = (String) request.getSession().getAttribute(UKDataContext.UKEFU_SYSTEM_INFOACQ) ;
    	if(!StringUtils.isBlank(inacq)){
    		request.getSession().removeAttribute(UKDataContext.UKEFU_SYSTEM_INFOACQ);
    	}else{
    		request.getSession().setAttribute(UKDataContext.UKEFU_SYSTEM_INFOACQ , "true");
    	}
    	return request(super.createRequestPageTempletResponse("redirect:/"));
    }
    
    @RequestMapping("/admin/auth/event")
    @Menu(type = "admin" , subtype = "authevent")
    public ModelAndView authevent(ModelMap map , HttpServletRequest request , @Valid String title , @Valid String url , @Valid String iconstr, @Valid String icontext) {
    	map.addAttribute("title", title) ;
    	map.addAttribute("url", url) ;
    	if(!StringUtils.isBlank(iconstr) && !StringUtils.isBlank(icontext)){
    		map.addAttribute("iconstr", iconstr.replaceAll(icontext, "&#x"+UKTools.string2HexString(icontext)+";")) ;
    	}
    	return request(super.createRequestPageTempletResponse("/admin/system/auth/event"));
    }
    
    @RequestMapping("/admin/auth/save")
    @Menu(type = "admin" , subtype = "authsave")
    public ModelAndView authsave(ModelMap map , HttpServletRequest request , @Valid String title , @Valid SysDic dic) {
    	SysDic sysDic = sysDicRes.findByCode(UKDataContext.UKEFU_SYSTEM_AUTH_DIC) ;
    	boolean newdic = false ;
    	if(sysDic!=null && !StringUtils.isBlank(dic.getName())){
    		if(!StringUtils.isBlank(dic.getParentid())){
    			if(dic.getParentid().equals("0")){
    				dic.setParentid(sysDic.getId());
    				newdic = true ;
    			}else{
    				List<SysDic> dicList = sysDicRes.findByDicid(sysDic.getId()) ;
    				for(SysDic temp : dicList){
    					if(temp.getCode().equals(dic.getParentid()) || temp.getName().equals(dic.getParentid())){
    						dic.setParentid(temp.getId());
    						newdic = true ;
    					}
    				}
    			}
    		}
    		if(newdic){
    			dic.setCreater(super.getUser(request).getId());
    			dic.setCreatetime(new Date());
    			dic.setCtype("auth");
    			dic.setDicid(sysDic.getId());
    			sysDicRes.save(dic) ;
    		}
    	}
    	return request(super.createRequestPageTempletResponse("/public/success"));
    }

}