<html>
    <head>
        <#assign pageTitle=action.getText('spam.admin.title') />
        <title>${pageTitle}</title>
        <content tag="pagetitle">${pageTitle}</content>
        <content tag="pageID">user-spam</content>
    </head>
    <body>
        <#include "/template/global/include/form-message.ftl" />

		<h2><@s.text name="spam.admin.resolve.description" /></h2>

		<#if unapprovedSpammers?has_content>
        <table>
        	<tr>
        		<th><@s.text name="spam.admin.resolve.user"/></th>
        		<th><@s.text name="spam.admin.resolve.action"/></th>
        	</tr>
			<#list unapprovedSpammers as spammer>
			<tr>
				<td><@jive.userDisplayNameLink user=spammer /></td>
				<td>
				<@s.form theme="simple" action="resolve-user-spam-content">
					<@s.hidden name="spammerUserName" value="${spammer.username}"/>
					<@s.submit value="${action.getText('spam.admin.resolve.submit')}" />
				</@s.form>
				<@s.form theme="simple" action="user-delete.jsp">
					<@s.hidden name="user" value="${spammer.ID?c}"/>
					<@s.submit value="${action.getText('spam.admin.resolve.delete')}" />
				</@s.form>
				</td>
			</tr>
			</#list>
		</table>
		<#else>
		<@s.text name="spam.admin.resolve.nodata"/>
		</#if>
    </body>
</html>