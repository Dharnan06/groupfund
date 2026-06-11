const API_BASE_URL = "http://localhost:8080";
const token = localStorage.getItem("token");
let savingsGroups = [];
let savingsLoading = false;

if (!token) window.location.href = "../auth/login.html";

const sidebar = document.getElementById("sidebar");
const menuBtn = document.getElementById("menuBtn");
if (menuBtn) menuBtn.addEventListener("click", () => sidebar.classList.toggle("show"));
document.addEventListener("click", e => {
  if (window.innerWidth <= 900 && sidebar && menuBtn && !sidebar.contains(e.target) && !menuBtn.contains(e.target)) sidebar.classList.remove("show");
});
document.querySelectorAll(".modal-overlay").forEach(modal => {
  modal.addEventListener("click", e => { if (e.target === modal) modal.style.display = "none"; });
});

function logout(){ localStorage.clear(); window.location.href = "../auth/login.html"; }
function formatCurrency(amount){ return `₹${Number(amount || 0).toLocaleString("en-IN")}`; }
function formatStatus(status){ const map={PENDING_MEMBERS:"Waiting for Members",READY_TO_ACTIVATE:"Ready to Activate",ACTIVATION_PENDING:"Activation Voting",ACTIVE:"Active",CLOSED:"Closed"}; return map[status] || status || "-"; }
function currentMonth(){ const d=new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}`; }
function monthDate(){ return `${currentMonth()}-01`; }
function formatMonth(value){
  if(!value) return "-";
  const d = new Date(`${value}T00:00:00`);
  if(Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString("en-IN", {month:"long", year:"numeric"});
}
function formatDate(value){
  if(!value) return "-";
  const d = new Date(`${value}T00:00:00`);
  if(Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString("en-IN", {day:"2-digit", month:"short", year:"numeric"});
}
function openSavingsModal(){
  document.getElementById("savingsModal").style.display="flex";
  if(!document.getElementById("startDate").value) document.getElementById("startDate").value=new Date().toISOString().slice(0,10);
  updateSavingsPreview();
}
function closeSavingsModal(){ document.getElementById("savingsModal").style.display="none"; }

async function api(url, options={}){
  const res = await fetch(url,{...options,headers:{"Content-Type":"application/json","Authorization":`Bearer ${token}`,...(options.headers||{})}});
  const text = await res.text();
  let data;
  try { data = text ? JSON.parse(text) : {}; }
  catch(_e) { throw new Error(`Server returned non-JSON response for ${url}`); }
  if(!res.ok || data.success===false) throw new Error(data.message || data.detail || "Request failed");
  return data;
}

function updateSavingsPreview(){
  const amount = Number(document.getElementById("monthlySavingsAmount")?.value || 0);
  const preview = document.getElementById("savingsPreviewAmount");
  if(preview) preview.textContent = formatCurrency(amount);
}

document.getElementById("monthlySavingsAmount")?.addEventListener("input", updateSavingsPreview);

async function createSavingsGroup(){
  const msg=document.getElementById("savingsMessage"); msg.textContent=""; msg.className="message";
  const groupName=document.getElementById("groupName").value.trim();
  const amount=Number(document.getElementById("monthlySavingsAmount").value);
  const startDate=document.getElementById("startDate").value;
  const targetMembers=Number(document.getElementById("targetMembers")?.value || 5);
  if(!groupName || !amount || amount<=0 || !startDate){ msg.textContent="Enter group name, amount and start date."; msg.className="message error"; return; }
  if(targetMembers < 5 || targetMembers > 20){ msg.textContent="Member limit must be between 5 and 20."; msg.className="message error"; return; }
  try{
    await api(`${API_BASE_URL}/api/groups/create`,{method:"POST",body:JSON.stringify({groupName,groupType:"SAVINGS",monthlySavingsAmount:amount,startDate,targetMembers})});
    msg.textContent="Savings group created successfully."; msg.className="message success";
    document.getElementById("groupName").value=""; document.getElementById("monthlySavingsAmount").value=""; if(document.getElementById("targetMembers")) document.getElementById("targetMembers").value="5";
    updateSavingsPreview();
    await loadSavings(true);
    setTimeout(closeSavingsModal,500);
  }catch(e){ msg.textContent=e.message; msg.className="message error"; }
}

function renderHistory(history){
  if(!history || history.length===0){
    return `<div class="history-empty">No savings payment recorded for you yet. The group leader can mark your monthly savings from the group Payments tab.</div>`;
  }
  return history.slice(0,8).map(p => `
    <div class="history-item">
      <div><strong>${formatMonth(p.paymentMonth)}</strong><span>${p.notes || "Monthly savings"}</span></div>
      <div><strong>${formatCurrency(p.amount)}</strong><span>${p.status || "PAID"}</span></div>
    </div>
  `).join("");
}

function getCurrentMonthPaidFromHistory(history, expected){
  const paid = (history || [])
    .filter(p => String(p.paymentMonth) === monthDate())
    .reduce((sum, p) => sum + Number(p.amount || 0), 0);
  return Math.min(paid, Number(expected || 0));
}

async function loadSavings(force=false){
  if(savingsLoading && !force) return;
  savingsLoading = true;
  const list=document.getElementById("savingsList");
  try{
    const groupsRes=await api(`${API_BASE_URL}/api/groups/my-groups`,{method:"GET"});
    const baseGroups=(groupsRes.data||[]).filter(g=>g.groupType==="SAVINGS");
    const detailed=await Promise.all(baseGroups.map(g=>api(`${API_BASE_URL}/api/groups/${g.groupId || g.id}`,{method:"GET"}).then(r=>r.data)));
    savingsGroups=detailed;
    let paid=0, pending=0, monthly=0;
    const rows=[];
    for(const g of detailed){
      const groupId = g.groupId || g.id;
      const expected=Number(g.savingsDetails?.monthlySavingsAmount || 0);
      monthly += expected;
      let history=[];
      try{
        const h=await api(`${API_BASE_URL}/api/payments/group/${groupId}/my-history`,{method:"GET"});
        history=h.data||[];
      }catch(_e){}
      const myPaid = getCurrentMonthPaidFromHistory(history, expected);
      const active = g.groupStatus === "ACTIVE";
      const myPending = active ? Math.max(expected - myPaid, 0) : 0;
      paid += myPaid;
      pending += myPending;
      rows.push(`
        <div class="group-row history-row ${myPending === 0 ? "paid-row" : "pending-row"}">
          <div class="group-main">
            <h3>${g.groupName}</h3>
            <p>${formatStatus(g.groupStatus)} • ${g.totalMembers}/${g.targetMembers || 20} members</p>
          </div>
          <div><span>Monthly Amount</span><strong>${formatCurrency(expected)}</strong></div>
          <div><span>My Paid This Month</span><strong>${formatCurrency(myPaid)}</strong></div>
          <div><span>My Pending This Month</span><strong>${formatCurrency(myPending)}</strong></div>
          <div><span>Start Date</span><strong>${formatDate(g.startDate)}</strong></div>
          <div><span>Status</span><strong class="status-text ${!active ? "locked" : (myPending === 0 ? "paid" : "pending")}">${!active ? "Locked" : (myPending === 0 ? "Paid" : "Pending")}</strong></div>
          <div class="history-block">
            <h4>My Savings History</h4>
            ${renderHistory(history)}
          </div>
        </div>`);
    }
    document.getElementById("savingsGroupCount").textContent=detailed.length;
    document.getElementById("totalMonthlySavings").textContent=formatCurrency(monthly);
    document.getElementById("thisMonthPaid").textContent=formatCurrency(paid);
    document.getElementById("thisMonthPending").textContent=formatCurrency(pending);
    list.innerHTML=rows.join("") || `<p class="empty-text">No savings groups yet. Click “Start Savings Group”.</p>`;
  }catch(e){ list.innerHTML=`<p class="empty-text">${e.message}</p>`; }
  finally { savingsLoading = false; }
}

loadSavings();
window.addEventListener("storage", event => { if(event.key === "groupFundLastPaymentUpdate") loadSavings(true); });
document.addEventListener("visibilitychange", () => { if(!document.hidden) loadSavings(true); });
setInterval(() => loadSavings(true), 5000);
