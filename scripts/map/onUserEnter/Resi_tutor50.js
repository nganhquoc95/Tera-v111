
function start() {
	ms.getClient().getSession().write(Packages.tools.packet.CField.UIPacket.IntroLock(0));
	ms.dispose();
	ms.openNpc(2159006);
}