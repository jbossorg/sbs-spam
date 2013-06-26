<html>
<head>
    <title><@s.text name="spam.report_spam.title" /></title>

    <meta name="nosidebar" content="true" />

    <content tag="breadcrumb">
		<@s.action name="legacy-breadcrumb" executeResult="true" ignoreContextParams="true">
		</@s.action>
    </content>

</head>
<body class="jive-body-formpage jive-body-formpage-discussion">

<!-- BEGIN header & intro  -->
<div id="jive-body-intro">
    <div id="jive-body-intro-content">
        <h1><@s.text name="spam.report_spam.title" /></h1>
        <p><@s.text name="spam.use_form_to_report.text" /></p>
    </div>
</div>
<!-- END header & intro -->


<!-- BEGIN main body -->
<div id="jive-body-main">

    <!-- BEGIN main body column -->
    <div id="jive-body-maincol-container">
        <div id="jive-body-maincol">



            <!-- BEGIN report abuse form block -->
            <div class="jive-box jive-box-form jive-standard-formblock-container">
                <div class="jive-box-body jive-standard-formblock">
                <#include "/template/global/include/form-message.ftl" />

                <form action="<@s.url action='report-spam'/>" name="abuseform" method="post">
                    <@jive.token name="report.spam.${objectType?c}.${objectID?c}"/>
                    <input type="hidden" name="objectID" value="${objectID?c}"/>
                    <input type="hidden" name="objectType" value="${objectType?c}"/>

                    <#if (disabled)>

                        <@s.text name="spam.already_reported.text"/>
                        <br/><br/>
                        <#-- Go Back -->
                        <input type="submit" name="method:cancel" value="<@s.text name="global.go_back" />">

                    <#else>

                        <@s.text name="spam.additionalComments.gtitle" />
                        <br/>

                        <textarea name="comment" cols="80" rows="8" wrap="virtual"></textarea>
                        <@macroFieldErrors name="comment"/>

                        <div class="jive-form-buttons">
                        <input type="submit" name="report" value="<@s.text name='spam.report_abuse.button' />">
                        <input type="submit" name="method:cancel" value="<@s.text name='global.cancel' />">
                        </div>

                    </#if>

                </form>

                </div>
            </div>
            <!-- END report abuse form block -->


        </div>
    </div>
    <!-- END main body column -->


</div>
<!-- END main body -->

</body>
</html>