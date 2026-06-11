const API_BASE_URL = window.location.origin;
const token = localStorage.getItem("token");
let loanGroups = [];
let loansLoading = false;

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
function openLoanModal(){
  document.getElementById("loanModal").style.display="flex";
  if(!document.getElementById("startDate").value) document.getElementById("startDate").value=new Date().toISOString().slice(0,10);
  calculateEmi();
}
function closeLoanModal(){ document.getElementById("loanModal").style.display="none"; }

async function api(url, options={}){
  const res = await fetch(url,{...options,headers:{"Content-Type":"application/json","Authorization":`Bearer ${token}`,...(options.headers||{})}});
  const text = await res.text();
  let data;
  try { data = text ? JSON.parse(text) : {}; }
  catch(_e) { throw new Error(`Server returned non-JSON response for ${url}`); }
  if(!res.ok || data.success===false) throw new Error(data.message || data.detail || "Request failed");
  return data;
}

function calculateEmi(){
  const principal=Number(document.getElementById("loanAmount")?.value||0);
  const months=Number(document.getElementById("durationMonths")?.value||0);
  const annualRate=Number(document.getElementById("interestRate")?.value||0);
  const preview = document.getElementById("emiPreviewAmount");
  if(principal<=0 || months<=0){ if(preview) preview.textContent = "₹0"; return; }
  let emi=principal/months;
  if(annualRate>0){
    const r=annualRate/12/100;
    emi=(principal*r*Math.pow(1+r,months))/(Math.pow(1+r,months)-1);
  }
  const rounded = Math.round(emi);
  document.getElementById("monthlyEmi").value=rounded;
  if(preview) preview.textContent = formatCurrency(rounded);
}

async function createLoanGroup(){
  const msg=document.getElementById("loanMessage"); msg.textContent=""; msg.className="message";
  const groupName=document.getElementById("groupName").value.trim();
  const loanSource=document.getElementById("loanSource").value.trim();
  const targetMembers=Number(document.getElementById("targetMembers")?.value || 5);
  const loanAmount=Number(document.getElementById("loanAmount").value);
  const durationMonths=Number(document.getElementById("durationMonths").value);
  const interestRate=Number(document.getElementById("interestRate").value||0);
  const monthlyEmi=Number(document.getElementById("monthlyEmi").value);
  const startDate=document.getElementById("startDate").value;
  if(!groupName || !loanSource || loanAmount<=0 || durationMonths<=0 || monthlyEmi<=0 || !startDate){ msg.textContent="Fill all loan details correctly."; msg.className="message error"; return; }
  if(targetMembers < 5 || targetMembers > 20){ msg.textContent="Member limit must be between 5 and 20."; msg.className="message error"; return; }
  try{
    await api(`${API_BASE_URL}/api/groups/create`,{method:"POST",body:JSON.stringify({groupName,groupType:"CUSTOM_LOAN",loanSource,loanAmount,durationMonths,interestRate,monthlyEmi,startDate,targetMembers})});
    msg.textContent="Loan group created successfully."; msg.className="message success";
    await loadLoans(true);
    setTimeout(closeLoanModal,500);
  }catch(e){ msg.textContent=e.message; msg.className="message error"; }
}

function renderHistory(history){
  if(!history || history.length===0){
    return `<div class="history-empty">No EMI payment recorded for you yet. The group leader can mark your EMI from the group Payments tab.</div>`;
  }
  return history.slice(0,8).map(p => `
    <div class="history-item">
      <div><strong>${formatMonth(p.paymentMonth)}</strong><span>${p.notes || "Monthly EMI"}</span></div>
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

async function loadLoans(force=false){
  if(loansLoading && !force) return;
  loansLoading = true;
  const list=document.getElementById("loanList");
  try{
    const groupsRes=await api(`${API_BASE_URL}/api/groups/my-groups`,{method:"GET"});
    const baseGroups=(groupsRes.data||[]).filter(g=>g.groupType==="CUSTOM_LOAN");
    const detailed=await Promise.all(baseGroups.map(g=>api(`${API_BASE_URL}/api/groups/${g.groupId || g.id}`,{method:"GET"}).then(r=>r.data)));
    loanGroups=detailed;
    let totalLoan=0,totalEmi=0,totalPending=0,totalPaid=0;
    const rows=[];
    for(const g of detailed){
      const groupId = g.groupId || g.id;
      const loan=Number(g.loanDetails?.loanAmount||0);
      const emi=Number(g.loanDetails?.monthlyEmi||0);
      totalLoan+=loan; totalEmi+=emi;
      let history=[];
      try{
        const h=await api(`${API_BASE_URL}/api/payments/group/${groupId}/my-history`,{method:"GET"});
        history=h.data||[];
      }catch(_e){}
      const paid = getCurrentMonthPaidFromHistory(history, emi);
      const active = g.groupStatus === "ACTIVE";
      const pending = active ? Math.max(emi - paid, 0) : 0;
      totalPaid += paid;
      totalPending += pending;
      rows.push(`
        <div class="group-row history-row ${pending === 0 ? "paid-row" : "pending-row"}">
          <div class="group-main"><h3>${g.groupName}</h3><p>${g.loanDetails?.loanSource || "Loan"} • ${formatStatus(g.groupStatus)} • ${g.totalMembers}/${g.targetMembers || 20} members</p></div>
          <div><span>Loan Amount</span><strong>${formatCurrency(loan)}</strong></div>
          <div><span>Monthly EMI</span><strong>${formatCurrency(emi)}</strong></div>
          <div><span>My Paid This Month</span><strong>${formatCurrency(paid)}</strong></div>
          <div><span>My Pending This Month</span><strong>${formatCurrency(pending)}</strong></div>
          <div><span>Status</span><strong class="status-text ${!active ? "locked" : (pending === 0 ? "paid" : "pending")}">${!active ? "Locked" : (pending === 0 ? "Paid" : "Pending")}</strong></div>
          <div class="history-block">
            <h4>My EMI History</h4>
            ${renderHistory(history)}
          </div>
        </div>`);
    }
    document.getElementById("loanGroupCount").textContent=detailed.length;
    document.getElementById("totalLoanAmount").textContent=formatCurrency(totalLoan);
    document.getElementById("totalMonthlyEmi").textContent=formatCurrency(totalEmi);
    document.getElementById("loanPending").textContent=formatCurrency(totalPending);
    list.innerHTML=rows.join("") || `<p class="empty-text">No loan groups yet. Click “Apply Loan”.</p>`;
  }catch(e){ list.innerHTML=`<p class="empty-text">${e.message}</p>`; }
  finally { loansLoading = false; }
}

loadLoans();
window.addEventListener("storage", event => { if(event.key === "groupFundLastPaymentUpdate") loadLoans(true); });
document.addEventListener("visibilitychange", () => { if(!document.hidden) loadLoans(true); });
setInterval(() => loadLoans(true), 5000);
